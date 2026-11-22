package com.ciudadalerta.data.repository

import com.ciudadalerta.data.model.Comentario
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ComentariosRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val comentariosCol = firestore.collection("comentarios")

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
}
