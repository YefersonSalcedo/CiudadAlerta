package com.ciudadalerta.ui.stats

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciudadalerta.data.model.Categoria
import com.ciudadalerta.data.model.EstadoIncidencia
import com.ciudadalerta.data.model.Incidencia
import com.ciudadalerta.data.repository.IncidenciasRepository
import com.ciudadalerta.data.util.GeoUtils
import com.ciudadalerta.ui.map.MapViewModel
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

    fun cargarEstadisticas(lat: Double = 7.5778, lng: Double = -75.4044) {
        _uiState.value = StatsUiState.Loading
        viewModelScope.launch {
            try {
                val todas = incidenciasRepository.obtenerTodasLasIncidencias()
                if (todas.isEmpty()) {
                    _uiState.value = StatsUiState.Empty
                    return@launch
                }

                val enZona = todas.filter { inc ->
                    GeoUtils.distanciaMetros(
                        lat, lng,
                        inc.ubicacion.latitud, inc.ubicacion.longitud
                    ) <= (MapViewModel.RADIO_ZONA_KM * 1000.0)
                }

                val ciudadDetectada = listOf("Tarazá", "Medellín", "Caucasia", "Bogotá", "Cali").firstOrNull { c ->
                    enZona.any { it.ubicacion.direccionAproximada.contains(c, ignoreCase = true) }
                } ?: "Tu ciudad"

                val listCiudad = if (enZona.isNotEmpty()) enZona else todas
                val statsCiudad = calcularStats(listCiudad, ciudadDetectada)
                val statsGlobal = calcularStats(todas, "Todas las zonas")

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

    private fun calcularStats(
        incidencias: List<Incidencia>,
        nombreCiudad: String
    ): IncidenciasStats {
        val activas = incidencias.count { it.estado == EstadoIncidencia.ACTIVA }
        val resueltas = incidencias.count { it.estado == EstadoIncidencia.RESUELTA }

        val resueltasConFechas = incidencias.filter { inc ->
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

        val agrupadoPorCategoria = incidencias.groupBy { it.categoria }
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

        return IncidenciasStats(
            totalActivas = activas,
            totalResueltas = resueltas,
            tiempoPromedioResolucionTexto = tiempoPromedioTexto,
            categoriaMasReportada = masReportada,
            conteoPorCategoria = conteoLista,
            ciudadNombre = nombreCiudad
        )
    }
}
