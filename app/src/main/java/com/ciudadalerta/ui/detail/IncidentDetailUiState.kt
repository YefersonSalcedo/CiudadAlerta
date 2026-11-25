package com.ciudadalerta.ui.detail

import com.ciudadalerta.data.model.Comentario
import com.ciudadalerta.data.model.Incidencia
import com.ciudadalerta.data.model.Reporte

enum class TipoVoto { NINGUNO, SIGUE_AHI, RESUELTO }

data class IncidentDetailUiState(
    val cargando: Boolean = false,
    val mensajeError: String? = null,
    val incidencia: Incidencia? = null,
    val comentarios: List<Comentario> = emptyList(),
    val reportes: List<Reporte> = emptyList(),
    val esMiReporte: Boolean = false,
    val siguiendo: Boolean = false,
    val yaVoto: Boolean = false,
    val tipoVoto: TipoVoto = TipoVoto.NINGUNO,
    val yaDenuncio: Boolean = false
)
