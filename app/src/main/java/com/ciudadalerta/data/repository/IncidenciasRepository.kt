package com.ciudadalerta.data.repository

import android.graphics.Bitmap
import com.ciudadalerta.data.config.CiudadAlertaConfig
import com.ciudadalerta.data.model.*
import com.ciudadalerta.data.util.GeoUtils
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

class IncidenciasRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    private val incidenciasCol = firestore.collection("incidencias")
    private val reportesCol = firestore.collection("reportes")
    private val comentariosCol = firestore.collection("comentarios")
    private val marcasResueltoCol = firestore.collection("marcasResuelto")
    private val denunciasCol = firestore.collection("denuncias")

    suspend fun subirImagenIncidencia(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val data = baos.toByteArray()
        val path = "incidencias/${System.currentTimeMillis()}_${(1000..9999).random()}.jpg"
        val ref = storage.reference.child(path)
        ref.putBytes(data).await()
        return ref.downloadUrl.await().toString()
    }

    fun observarIncidenciasActivas(): Flow<List<Incidencia>> = callbackFlow {
        val listener = incidenciasCol
            .whereEqualTo("ocultoDelMapa", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val lista = snapshot?.toObjects(Incidencia::class.java) ?: emptyList()
                trySend(lista)
            }
        awaitClose { listener.remove() }
    }

    suspend fun buscarSimilares(
        latitud: Double,
        longitud: Double,
        categoria: String,
        radioMetros: Double
    ): List<Incidencia> {
        val snapshot = incidenciasCol
            .whereEqualTo("estado", EstadoIncidencia.ACTIVA.name)
            .whereEqualTo("categoria", categoria)
            .whereEqualTo("ocultoDelMapa", false)
            .get()
            .await()

        return snapshot.toObjects(Incidencia::class.java).filter { inc ->
            GeoUtils.distanciaMetros(latitud, longitud, inc.ubicacion.latitud, inc.ubicacion.longitud) <= radioMetros
        }
    }

    suspend fun crearNuevaIncidencia(incidencia: Incidencia, reporteInicial: Reporte): String {
        val docRef = incidenciasCol.document()
        val incidenciaCompleta = incidencia.copy(
            id = docRef.id,
            ubicacion = incidencia.ubicacion.copy(
                geohash = GeoUtils.encodeGeohash(incidencia.ubicacion.latitud, incidencia.ubicacion.longitud)
            ),
            reportantesIds = listOf(reporteInicial.usuarioId)
        )
        docRef.set(incidenciaCompleta).await()
        reportesCol.document().set(reporteInicial.copy(incidenciaId = docRef.id)).await()
        return docRef.id
    }

    suspend fun unirReporteAIncidencia(incidenciaId: String, reporte: Reporte) {
        val incRef = incidenciasCol.document(incidenciaId)
        firestore.runTransaction { tx ->
            val snap = tx.get(incRef)
            val contador = snap.getLong("contadorReportes") ?: 1
            tx.update(incRef, "contadorReportes", contador + 1)
            tx.update(incRef, "reportantesIds", FieldValue.arrayUnion(reporte.usuarioId))
        }.await()
        reportesCol.document().set(reporte.copy(incidenciaId = incidenciaId)).await()
    }

    suspend fun marcarResuelto(incidenciaId: String, usuarioId: String): Boolean {
        val marcaId = "${usuarioId}_${incidenciaId}"
        val marcaRef = marcasResueltoCol.document(marcaId)
        val incRef = incidenciasCol.document(incidenciaId)

        if (marcaRef.get().await().exists()) return false

        firestore.runBatch { batch ->
            batch.set(marcaRef, MarcaResuelto(usuarioId, incidenciaId))
            batch.update(incRef, "marcasResuelto", FieldValue.increment(1))
        }.await()

        val snap = incRef.get().await()
        val total = snap.getLong("marcasResuelto") ?: 0
        if (total >= CiudadAlertaConfig.MARCAS_PARA_RESOLVER_N) {
            incRef.update(
                mapOf(
                    "estado" to EstadoIncidencia.RESUELTA.name,
                    "fechaResuelta" to System.currentTimeMillis()
                )
            ).await()
        }
        return true
    }

    suspend fun marcarSigueAhi(incidenciaId: String) {
        val incRef = incidenciasCol.document(incidenciaId)
        val snap = incRef.get().await()
        val estado = snap.getString("estado")

        val updates = mutableMapOf<String, Any>("contadorReportes" to FieldValue.increment(1))
        if (estado == EstadoIncidencia.RESUELTA.name) {
            updates["estado"] = EstadoIncidencia.ACTIVA.name
            updates["marcasResuelto"] = 0
            updates["fechaResuelta"] = FieldValue.delete()
        }
        incRef.update(updates).await()
    }

    suspend fun denunciarContenido(
        contenidoId: String,
        usuarioId: String,
        motivo: MotivoDenuncia,
        detalles: String?
    ): Boolean {
        val docId = "${usuarioId}_${contenidoId}"
        val ref = denunciasCol.document(docId)
        if (ref.get().await().exists()) return false

        ref.set(Denuncia(usuarioId, contenidoId, motivo, detalles)).await()
        return true
    }

    fun observarComentarios(incidenciaId: String): Flow<List<Comentario>> = callbackFlow {
        val listener = comentariosCol
            .whereEqualTo("incidenciaId", incidenciaId)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snap?.toObjects(Comentario::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun publicarComentario(comentario: Comentario) {
        comentariosCol.document().set(comentario).await()
    }

    suspend fun obtenerIncidenciaPorId(incidenciaId: String): Incidencia? {
        val snap = incidenciasCol.document(incidenciaId).get().await()
        return snap.toObject(Incidencia::class.java)
    }

    fun observarIncidenciasPorUsuario(usuarioId: String): Flow<List<Incidencia>> = callbackFlow {
        val listener = incidenciasCol
            .whereArrayContains("reportantesIds", usuarioId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snap?.toObjects(Incidencia::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }
}
