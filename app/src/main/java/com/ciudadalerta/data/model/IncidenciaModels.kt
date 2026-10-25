package com.ciudadalerta.data.model

import com.google.firebase.firestore.DocumentId

enum class EstadoIncidencia { ACTIVA, RESUELTA, ARCHIVADA }
enum class MotivoDenuncia { INAPROPIADO, NO_ES_INCIDENCIA, EMERGENCIA_VITAL }

data class Ubicacion(
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val direccionAproximada: String = "",
    val geohash: String = ""
)

data class Usuario(
    @DocumentId val id: String = "",
    val nombre: String = "",
    val correo: String = "",
    val fotoUrl: String? = null,
    val puntosCivicos: Int = 0,
    val nivel: Int = 1,
    val fcmToken: String? = null
)

data class Incidencia(
    @DocumentId val id: String = "",
    val titulo: String = "",
    val categoria: String = Categoria.Otros.nombre,
    val ubicacion: Ubicacion = Ubicacion(),
    val contadorReportes: Int = 1,
    val estado: EstadoIncidencia = EstadoIncidencia.ACTIVA,
    val fotoPrincipalUrl: String = "",
    val marcasResuelto: Int = 0,
    val denunciasCount: Int = 0,
    val reportantesIds: List<String> = emptyList(),
    val seguidoresIds: List<String> = emptyList(),
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaResuelta: Long? = null,
    val ultimaVerificacionNotificada: Long = System.currentTimeMillis(),
    val severidadEstimada: String = "Media",
    val ocultoDelMapa: Boolean = false
)

data class Reporte(
    @DocumentId val id: String = "",
    val incidenciaId: String = "",
    val usuarioId: String = "",
    val usuarioNombre: String = "",
    val fotoUrl: String = "",
    val descripcion: String = "",
    val fecha: Long = System.currentTimeMillis()
)

data class Comentario(
    @DocumentId val id: String = "",
    val incidenciaId: String = "",
    val usuarioId: String = "",
    val usuarioNombre: String = "",
    val usuarioFotoUrl: String? = null,
    val texto: String = "",
    val fecha: Long = System.currentTimeMillis(),
    val utilCount: Int = 0
)

data class MarcaResuelto(
    val usuarioId: String = "",
    val contenidoId: String = "",
    val fecha: Long = System.currentTimeMillis()
)

data class Denuncia(
    val usuarioId: String = "",
    val contenidoId: String = "",
    val motivo: MotivoDenuncia = MotivoDenuncia.INAPROPIADO,
    val detalles: String? = null,
    val fecha: Long = System.currentTimeMillis()
)

data class GeminiAnalisisResponse(
    val categoria: String,
    val estado: String,
    val motivoBloqueo: String?,
    val descripcion: String,
    val esEmergencia: Boolean
)
