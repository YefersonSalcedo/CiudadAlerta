package com.ciudadalerta.ui.stats

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciudadalerta.data.model.Categoria
import com.ciudadalerta.data.model.EstadoIncidencia
import com.ciudadalerta.data.repository.IncidenciasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class CategoriaConteo(
    val categoriaNombre: String,
    val cantidad: Int,
    val proporcion: Float,
    val colorHex: Long,
    val icono: ImageVector
)

data class IncidenciasStats(
    val totalActivas: Int,
    val totalResueltas: Int,
    val tiempoPromedioResolucionTexto: String,
    val categoriaMasReportada: String,
    val conteoPorCategoria: List<CategoriaConteo>
)

sealed interface StatsUiState {
    object Loading : StatsUiState
    object Empty : StatsUiState
    data class Success(val stats: IncidenciasStats) : StatsUiState
    data class Error(val mensaje: String) : StatsUiState
}

class StatsViewModel(
    private val incidenciasRepository: IncidenciasRepository = IncidenciasRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<StatsUiState>(StatsUiState.Loading)
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        cargarEstadisticas()
    }

    fun cargarEstadisticas() {
        _uiState.value = StatsUiState.Loading
        viewModelScope.launch {
            try {
                val todas = incidenciasRepository.obtenerTodasLasIncidencias()
                if (todas.isEmpty()) {
                    _uiState.value = StatsUiState.Empty
                    return@launch
                }

                val activas = todas.count { it.estado == EstadoIncidencia.ACTIVA }
                val resueltas = todas.count { it.estado == EstadoIncidencia.RESUELTA }

                val resueltasConFechas = todas.filter { inc ->
                    inc.estado == EstadoIncidencia.RESUELTA &&
                            inc.fechaResuelta != null &&
                            inc.fechaResuelta > inc.fechaCreacion
                }

                val tiempoPromedioTexto = if (resueltasConFechas.isNotEmpty()) {
                    val sumaMs = resueltasConFechas.sumOf { (it.fechaResuelta ?: 0L) - it.fechaCreacion }
                    val promedioMs = sumaMs / resueltasConFechas.size
                    val horas = promedioMs / (1000 * 3600.0)
                    val dias = horas / 24.0
                    if (dias >= 1.0) {
                        String.format(Locale.forLanguageTag("es-CO"), "%.1f días", dias)
                    } else {
                        String.format(Locale.forLanguageTag("es-CO"), "%.1f h", horas)
                    }
                } else {
                    "Sin datos"
                }

                val agrupadoPorCategoria = todas.groupBy { it.categoria }
                val masReportada = agrupadoPorCategoria.maxByOrNull { entry ->
                    entry.value.sumOf { it.contadorReportes }
                }?.key ?: "Ninguna"

                val maximoCantidad = agrupadoPorCategoria.values.maxOfOrNull { it.size } ?: 1

                val conteoLista = Categoria.CATALOGO.map { cat ->
                    val cants = agrupadoPorCategoria[cat.nombre]?.size ?: 0
                    CategoriaConteo(
                        categoriaNombre = cat.nombre,
                        cantidad = cants,
                        proporcion = if (maximoCantidad > 0) cants.toFloat() / maximoCantidad.toFloat() else 0f,
                        colorHex = cat.colorHex,
                        icono = cat.icono
                    )
                }.sortedByDescending { it.cantidad }

                _uiState.value = StatsUiState.Success(
                    IncidenciasStats(
                        totalActivas = activas,
                        totalResueltas = resueltas,
                        tiempoPromedioResolucionTexto = tiempoPromedioTexto,
                        categoriaMasReportada = masReportada,
                        conteoPorCategoria = conteoLista
                    )
                )
            } catch (e: Exception) {
                _uiState.value = StatsUiState.Error(
                    e.localizedMessage ?: "Error al cargar estadísticas."
                )
            }
        }
    }
}
