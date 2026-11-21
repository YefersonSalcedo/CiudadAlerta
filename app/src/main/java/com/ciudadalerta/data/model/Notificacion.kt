package com.ciudadalerta.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Tipos soportados para las notificaciones generadas por el sistema.
 */
object TipoNotificacion {
    const val INCIDENCIA_ACTUALIZADA = "INCIDENCIA_ACTUALIZADA"
    const val REVERIFICACION = "REVERIFICACION"
}

/**
 * Modelo de datos para las notificaciones de usuario guardadas en Firestore
 * en la ruta: usuarios/{uid}/notificaciones/{id}
 */
data class Notificacion(
    @DocumentId val id: String = "",
    val tipo: String = TipoNotificacion.INCIDENCIA_ACTUALIZADA,
    val incidenciaId: String = "",
    val titulo: String = "",
    val cuerpo: String = "",
    val fecha: Long = System.currentTimeMillis(),
    val leida: Boolean = false
)
