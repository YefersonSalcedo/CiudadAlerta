package com.ciudadalerta.data.repository

import android.graphics.Bitmap
import com.ciudadalerta.data.config.CiudadAlertaConfig
import com.ciudadalerta.data.model.*
import com.ciudadalerta.data.util.GeoUtils
import com.ciudadalerta.data.util.calcularNuevosPuntos
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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
                val lista = snapshot?.toObjects(Incidencia::class.java)
                    ?.filter { it.esVisible() } ?: emptyList()
                trySend(lista)
            }
        awaitClose { listener.remove() }
    }

    suspend fun buscarSimilares(
        latitud: Double,
        longitud: Double,
        categoria: String,
        radioMetros: Double = CiudadAlertaConfig.RADIO_AGRUPACION_DEFAULT_METROS
    ): List<Incidencia> {
        val snapshot = incidenciasCol
            .whereEqualTo("estado", EstadoIncidencia.ACTIVA.name)
            .whereEqualTo("categoria", categoria)
            .whereEqualTo("ocultoDelMapa", false)
            .get()
            .await()

        return snapshot.toObjects(Incidencia::class.java).filter { inc ->
            inc.esVisible() && GeoUtils.distanciaMetros(latitud, longitud, inc.ubicacion.latitud, inc.ubicacion.longitud) <= radioMetros
        }
    }

    suspend fun crearNuevaIncidencia(incidencia: Incidencia, reporteInicial: Reporte): String {
        val hace24h = System.currentTimeMillis() - 24 * 3600 * 1000L
        val reportesRecientes = reportesCol
            .whereEqualTo("usuarioId", reporteInicial.usuarioId)
            .whereGreaterThanOrEqualTo("fecha", hace24h)
            .get()
            .await()

        if (reportesRecientes.size() >= CiudadAlertaConfig.MAX_REPORTES_POR_USUARIO_DIA) {
            throw IllegalStateException(
                "Has alcanzado el límite máximo de ${CiudadAlertaConfig.MAX_REPORTES_POR_USUARIO_DIA} reportes en 24 horas."
            )
        }

        val docRef = incidenciasCol.document()
        val reporteRef = reportesCol.document()
        val userRef = firestore.collection("usuarios").document(reporteInicial.usuarioId)

        val incidenciaCompleta = incidencia.copy(
            id = docRef.id,
            ubicacion = incidencia.ubicacion.copy(
                geohash = GeoUtils.encodeGeohash(incidencia.ubicacion.latitud, incidencia.ubicacion.longitud)
            ),
            reportantesIds = listOf(reporteInicial.usuarioId),
            descripcion = reporteInicial.descripcion
        )

        firestore.runTransaction { tx ->
            // Reads first
            val userSnap = tx.get(userRef)
            val puntosResult = calcularNuevosPuntos(userSnap, CiudadAlertaConfig.PUNTOS_CREAR_INCIDENCIA, esInteraccion = false)

            // Writes after all reads
            tx.set(docRef, incidenciaCompleta)
            tx.set(reporteRef, reporteInicial.copy(incidenciaId = docRef.id))
            if (puntosResult != null) {
                tx.update(
                    userRef,
                    mapOf(
                        "puntosCivicos" to puntosResult.puntosCivicos,
                        "nivel" to puntosResult.nivel,
                        "puntosInteraccionHoy" to puntosResult.puntosInteraccionHoy,
                        "fechaUltimaInteraccion" to puntosResult.fechaUltimaInteraccion
                    )
                )
            }
        }.await()

        return docRef.id
    }

    suspend fun unirReporteAIncidencia(incidenciaId: String, reporte: Reporte) {
        val incRef = incidenciasCol.document(incidenciaId)
        val reporteRef = reportesCol.document()
        val userRef = firestore.collection("usuarios").document(reporte.usuarioId)

        firestore.runTransaction { tx ->
            // Reads first
            val incSnap = tx.get(incRef)
            val userSnap = tx.get(userRef)
            val puntosResult = calcularNuevosPuntos(userSnap, CiudadAlertaConfig.PUNTOS_UNIR_REPORTE, esInteraccion = false)

            // Writes after all reads
            val contador = incSnap.getLong("contadorReportes") ?: 1
            tx.update(incRef, "contadorReportes", contador + 1)
            tx.update(incRef, "reportantesIds", FieldValue.arrayUnion(reporte.usuarioId))
            tx.set(reporteRef, reporte.copy(incidenciaId = incidenciaId))
            if (puntosResult != null) {
                tx.update(
                    userRef,
                    mapOf(
                        "puntosCivicos" to puntosResult.puntosCivicos,
                        "nivel" to puntosResult.nivel,
                        "puntosInteraccionHoy" to puntosResult.puntosInteraccionHoy,
                        "fechaUltimaInteraccion" to puntosResult.fechaUltimaInteraccion
                    )
                )
            }
        }.await()
    }

    suspend fun marcarResuelto(incidenciaId: String, usuarioId: String): Boolean {
        val marcaId = "${usuarioId}_${incidenciaId}"
        val marcaRef = marcasResueltoCol.document(marcaId)
        val incRef = incidenciasCol.document(incidenciaId)
        val userRef = firestore.collection("usuarios").document(usuarioId)

        val success = firestore.runTransaction { tx ->
            // Reads first
            val marcaSnap = tx.get(marcaRef)
            if (marcaSnap.exists()) {
                return@runTransaction false
            }
            val incSnap = tx.get(incRef)
            val userSnap = tx.get(userRef)
            val puntosResult = calcularNuevosPuntos(userSnap, CiudadAlertaConfig.PUNTOS_MARCAR_RESUELTO, esInteraccion = false)

            // Writes after all reads
            tx.set(marcaRef, MarcaResuelto(usuarioId, incidenciaId))
            val marcasActuales = incSnap.getLong("marcasResuelto") ?: 0
            val nuevasMarcas = marcasActuales + 1
            val updates = mutableMapOf<String, Any>(
                "marcasResuelto" to nuevasMarcas
            )
            if (nuevasMarcas >= CiudadAlertaConfig.MARCAS_PARA_RESOLVER_N) {
                updates["estado"] = EstadoIncidencia.RESUELTA.name
                updates["fechaResuelta"] = System.currentTimeMillis()
            }
            tx.update(incRef, updates)
            if (puntosResult != null) {
                tx.update(
                    userRef,
                    mapOf(
                        "puntosCivicos" to puntosResult.puntosCivicos,
                        "nivel" to puntosResult.nivel,
                        "puntosInteraccionHoy" to puntosResult.puntosInteraccionHoy,
                        "fechaUltimaInteraccion" to puntosResult.fechaUltimaInteraccion
                    )
                )
            }
            true
        }.await()

        return success
    }

    companion object {
        private val confirmacionesCache = mutableSetOf<String>()
    }

    suspend fun yaMarcoResuelto(incidenciaId: String, usuarioId: String): Boolean {
        if (usuarioId.isBlank()) return false
        val marcaId = "${usuarioId}_${incidenciaId}"
        return marcasResueltoCol.document(marcaId).get().await().exists()
    }

    fun yaMarcoSigueAhi(incidenciaId: String, usuarioId: String): Boolean {
        if (usuarioId.isBlank()) return false
        val key = "${usuarioId}_${incidenciaId}"
        if (confirmacionesCache.contains(key)) return true
        val fueGuardado = obtenerPrefs()?.getBoolean(key, false) ?: false
        if (fueGuardado) confirmacionesCache.add(key)
        return fueGuardado
    }

    private fun guardarConfirmacionEnPrefs(usuarioId: String, incidenciaId: String) {
        try {
            val key = "${usuarioId}_${incidenciaId}"
            obtenerPrefs()?.edit()?.putBoolean(key, true)?.apply()
        } catch (_: Exception) {}
    }

    private fun obtenerPrefs(): android.content.SharedPreferences? {
        return try {
            val appClass = Class.forName("android.app.ActivityThread")
            val currentAppMethod = appClass.getMethod("currentApplication")
            val app = currentAppMethod.invoke(null) as? android.content.Context
            app?.getSharedPreferences("ciudad_alerta_votos", android.content.Context.MODE_PRIVATE)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun marcarSigueAhi(incidenciaId: String, usuarioId: String = "") {
        if (usuarioId.isNotBlank()) {
            val key = "${usuarioId}_${incidenciaId}"
            confirmacionesCache.add(key)
            guardarConfirmacionEnPrefs(usuarioId, incidenciaId)
        }
        val incRef = incidenciasCol.document(incidenciaId)
        val userRef = if (usuarioId.isNotBlank()) firestore.collection("usuarios").document(usuarioId) else null

        firestore.runTransaction { transaction ->
            // Reads first
            val snapshot = transaction.get(incRef)
            check(snapshot.exists()) {
                "La incidencia ya no existe."
            }
            val userSnap = if (userRef != null) transaction.get(userRef) else null
            val puntosResult = if (userSnap != null) calcularNuevosPuntos(userSnap, CiudadAlertaConfig.PUNTOS_CONFIRMAR_SIGUE_AHI, esInteraccion = true) else null

            // Writes after all reads
            val estado = snapshot.getString("estado")
            val updates = mutableMapOf<String, Any>(
                "contadorConfirmaciones" to FieldValue.increment(1),
                "ultimaVerificacionNotificada" to System.currentTimeMillis()
            )

            if (estado == EstadoIncidencia.RESUELTA.name) {
                updates["estado"] = EstadoIncidencia.ACTIVA.name
                updates["marcasResuelto"] = 0
                updates["fechaResuelta"] = FieldValue.delete()
            }

            transaction.update(incRef, updates)

            if (userRef != null && puntosResult != null) {
                transaction.update(
                    userRef,
                    mapOf(
                        "puntosCivicos" to puntosResult.puntosCivicos,
                        "nivel" to puntosResult.nivel,
                        "puntosInteraccionHoy" to puntosResult.puntosInteraccionHoy,
                        "fechaUltimaInteraccion" to puntosResult.fechaUltimaInteraccion
                    )
                )
            }
        }.await()
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

        val incRef = incidenciasCol.document(contenidoId)
        val incSnap = incRef.get().await()
        if (incSnap.exists()) {
            val totalDenuncias = (incSnap.getLong("denunciasCount") ?: 0) + 1
            val updates = mutableMapOf<String, Any>("denunciasCount" to totalDenuncias)
            if (totalDenuncias >= CiudadAlertaConfig.DENUNCIAS_PARA_OCULTAR_M) {
                updates["ocultoDelMapa"] = true
            }
            incRef.update(updates).await()
        }
        return true
    }

    fun observarIncidenciaPorId(incidenciaId: String): Flow<Incidencia?> = callbackFlow {
        val listener = incidenciasCol.document(incidenciaId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snap?.toObject(Incidencia::class.java))
            }
        awaitClose { listener.remove() }
    }

    fun observarReportes(incidenciaId: String): Flow<List<Reporte>> = callbackFlow {
        val listener = reportesCol
            .whereEqualTo("incidenciaId", incidenciaId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val lista = snap?.toObjects(Reporte::class.java)
                    ?.sortedByDescending { it.fecha } ?: emptyList()
                trySend(lista)
            }
        awaitClose { listener.remove() }
    }

    suspend fun obtenerIncidenciaPorId(incidenciaId: String): Incidencia? {
        val snap = incidenciasCol.document(incidenciaId).get().await()
        return snap.toObject(Incidencia::class.java)
    }

    suspend fun obtenerTodasLasIncidencias(): List<Incidencia> {
        val snap = incidenciasCol.get().await()
        return snap.toObjects(Incidencia::class.java)
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

    suspend fun obtenerEstadisticasPorCiudad(ciudad: String): EstadisticasData {
        val queryBase = incidenciasCol.whereEqualTo("ocultoDelMapa", false)
        val queryCiudad = if (ciudad.isNotBlank() && ciudad != "Todas las zonas") {
            queryBase.whereEqualTo("ciudad", ciudad)
        } else {
            queryBase
        }

        val total = queryCiudad.count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count
        val activas = queryCiudad.whereEqualTo("estado", EstadoIncidencia.ACTIVA.name).count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count
        val resueltas = queryCiudad.whereEqualTo("estado", EstadoIncidencia.RESUELTA.name).count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count

        val conteoPorCategoria = mutableMapOf<String, Long>()
        for (cat in Categoria.CATALOGO) {
            val countCat = queryCiudad.whereEqualTo("categoria", cat.nombre).count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count
            conteoPorCategoria[cat.nombre] = countCat
        }

        val snapResueltas = queryCiudad.whereEqualTo("estado", EstadoIncidencia.RESUELTA.name).get().await()
        val listaResueltas = snapResueltas.toObjects(Incidencia::class.java).filter { it.fechaResuelta != null && it.fechaResuelta > it.fechaCreacion }
        val tiempoPromedioMs = if (listaResueltas.isNotEmpty()) {
            listaResueltas.sumOf { (it.fechaResuelta ?: 0L) - it.fechaCreacion } / listaResueltas.size
        } else {
            0L
        }

        return EstadisticasData(
            total = total,
            activas = activas,
            resueltas = resueltas,
            conteoPorCategoria = conteoPorCategoria,
            tiempoPromedioMs = tiempoPromedioMs
        )
    }
}

data class EstadisticasData(
    val total: Long,
    val activas: Long,
    val resueltas: Long,
    val conteoPorCategoria: Map<String, Long>,
    val tiempoPromedioMs: Long
)
