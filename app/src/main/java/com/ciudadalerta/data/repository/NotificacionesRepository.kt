package com.ciudadalerta.data.repository

import com.ciudadalerta.data.model.Incidencia
import com.ciudadalerta.data.model.Notificacion
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class NotificacionesRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val incidenciasCol = firestore.collection("incidencias")
    private val usuariosCol = firestore.collection("usuarios")

    /**
     * Agrega el ID del usuario a la lista de seguidores de una incidencia.
     */
    suspend fun seguirIncidencia(incidenciaId: String, uid: String) {
        incidenciasCol.document(incidenciaId)
            .update("seguidoresIds", FieldValue.arrayUnion(uid))
            .await()
    }

    /**
     * Remueve el ID del usuario de la lista de seguidores de una incidencia.
     */
    suspend fun dejarDeSeguir(incidenciaId: String, uid: String) {
        incidenciasCol.document(incidenciaId)
            .update("seguidoresIds", FieldValue.arrayRemove(uid))
            .await()
    }

    /**
     * Observa en tiempo real si un usuario específico sigue una incidencia.
     */
    fun observarSeguimiento(incidenciaId: String, uid: String): Flow<Boolean> = callbackFlow {
        val listener = incidenciasCol.document(incidenciaId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(false)
                    return@addSnapshotListener
                }
                val incidencia = snapshot?.toObject(Incidencia::class.java)
                val estaSiguiendo = incidencia?.seguidoresIds?.contains(uid) == true
                trySend(estaSiguiendo)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Observa las notificaciones recibidas por el usuario ordenadas por fecha descendente.
     */
    fun observarNotificaciones(uid: String): Flow<List<Notificacion>> = callbackFlow {
        val listener = usuariosCol.document(uid)
            .collection("notificaciones")
            .orderBy("fecha", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val lista = snapshot?.toObjects(Notificacion::class.java) ?: emptyList()
                trySend(lista)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Marca una notificación como leída actualizando el campo `leida` a true.
     */
    suspend fun marcarComoLeida(uid: String, notificacionId: String) {
        usuariosCol.document(uid)
            .collection("notificaciones")
            .document(notificacionId)
            .update("leida", true)
            .await()
    }

    /**
     * Observa las incidencias seguidas por un usuario específico ordenadas en el cliente por fechaCreacion descendente.
     */
    fun observarIncidenciasSeguidas(uid: String): Flow<List<Incidencia>> = callbackFlow {
        val listener = incidenciasCol
            .whereArrayContains("seguidoresIds", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val lista = snapshot?.toObjects(Incidencia::class.java)
                    ?.sortedByDescending { it.fechaCreacion } ?: emptyList()
                trySend(lista)
            }
        awaitClose { listener.remove() }
    }
}
