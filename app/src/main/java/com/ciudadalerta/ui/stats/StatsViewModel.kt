package com.ciudadalerta.ui.stats

import android.content.Context
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciudadalerta.data.model.Categoria
import com.ciudadalerta.data.model.extraerCiudadDeDireccion
import com.ciudadalerta.data.repository.EstadisticasData
import com.ciudadalerta.data.repository.IncidenciasRepository
import com.ciudadalerta.data.util.obtenerDireccion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    val conteoPorCategoria: List<CategoriaConteo>,
    val ciudadNombre: String = "Tu ciudad"
)

sealed interface StatsUiState {
    object Loading : StatsUiState
    object Empty : StatsUiState
    data class Success(
        val statsCiudad: IncidenciasStats,
        val statsGlobal: IncidenciasStats,
        val filtroSoloCiudad: Boolean = true
    ) : StatsUiState
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

    fun cargarEstadisticas(lat: Double = 7.5778, lng: Double = -75.4044, context: Context? = null) {
        _uiState.value = StatsUiState.Loading
        viewModelScope.launch {
            try {
                val ciudadActual = if (context != null) {
                    val dir = obtenerDireccion(context, lat, lng)
                    extraerCiudadDeDireccion(dir).ifBlank { "Tarazá" }
                } else {
                    "Tarazá"
                }

                val statsCiudadData = incidenciasRepository.obtenerEstadisticasPorCiudad(ciudadActual)
                val statsGlobalData = incidenciasRepository.obtenerEstadisticasPorCiudad("Todas las zonas")

                if (statsCiudadData.total == 0L && statsGlobalData.total == 0L) {
                    _uiState.value = StatsUiState.Empty
                    return@launch
                }

                val statsCiudad = construirStats(statsCiudadData, ciudadActual)
                val statsGlobal = construirStats(statsGlobalData, "Todas las zonas")

                _uiState.value = StatsUiState.Success(
                    statsCiudad = statsCiudad,
                    statsGlobal = statsGlobal,
                    filtroSoloCiudad = true
                )
            } catch (e: Exception) {
                _uiState.value = StatsUiState.Error(
                    e.localizedMessage ?: "Error al cargar estadísticas."
                )
            }
        }
    }

    fun setFiltroSoloCiudad(soloCiudad: Boolean) {
        _uiState.update { state ->
            if (state is StatsUiState.Success) {
                state.copy(filtroSoloCiudad = soloCiudad)
            } else {
                state
            }
        }
    }

    private fun construirStats(
        data: EstadisticasData,
        nombreCiudad: String
    ): IncidenciasStats {
        val tiempoPromedioTexto = if (data.tiempoPromedioMs > 0) {
            val horas = data.tiempoPromedioMs / (1000 * 3600.0)
            val dias = horas / 24.0
            if (dias >= 1.0) {
                String.format(Locale.forLanguageTag("es-CO"), "%.1f días", dias)
            } else {
                String.format(Locale.forLanguageTag("es-CO"), "%.1f h", horas)
            }
        } else {
            "Sin datos"
        }

        val masReportada = data.conteoPorCategoria.maxByOrNull { it.value }?.key ?: "Ninguna"
        val maximoCantidad = data.conteoPorCategoria.values.maxOfOrNull { it }?.toInt() ?: 1

        val conteoLista = Categoria.CATALOGO.map { cat ->
            val cants = (data.conteoPorCategoria[cat.nombre] ?: 0L).toInt()
            CategoriaConteo(
                categoriaNombre = cat.nombre,
                cantidad = cants,
                proporcion = if (maximoCantidad > 0) cants.toFloat() / maximoCantidad.toFloat() else 0f,
                colorHex = cat.colorHex,
                icono = cat.icono
            )
        }.sortedByDescending { it.cantidad }

        return IncidenciasStats(
            totalActivas = data.activas.toInt(),
            totalResueltas = data.resueltas.toInt(),
            tiempoPromedioResolucionTexto = tiempoPromedioTexto,
            categoriaMasReportada = masReportada,
            conteoPorCategoria = conteoLista,
            ciudadNombre = nombreCiudad
        )
    }
}
