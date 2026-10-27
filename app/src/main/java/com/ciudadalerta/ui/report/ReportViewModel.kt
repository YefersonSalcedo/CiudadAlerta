package com.ciudadalerta.ui.report

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciudadalerta.data.ai.GeminiAnalysisService
import com.ciudadalerta.data.config.CiudadAlertaConfig
import com.ciudadalerta.data.model.*
import com.ciudadalerta.data.repository.IncidenciasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AnalisisUiState {
    object Idle : AnalisisUiState
    object Loading : AnalisisUiState
    data class Success(val resultado: GeminiAnalisisResponse) : AnalisisUiState
    data class Error(val mensaje: String) : AnalisisUiState
}

sealed interface GuardadoUiState {
    object Idle : GuardadoUiState
    object Loading : GuardadoUiState
    data class Success(val incidenciaId: String) : GuardadoUiState
    data class Error(val mensaje: String) : GuardadoUiState
}

class ReportViewModel(
    private val geminiService: GeminiAnalysisService = GeminiAnalysisService(),
    private val incidenciasRepository: IncidenciasRepository = IncidenciasRepository()
) : ViewModel() {

    private val _fotoCaptured = MutableStateFlow<Bitmap?>(null)
    val fotoCaptured: StateFlow<Bitmap?> = _fotoCaptured.asStateFlow()

    private val _descripcion = MutableStateFlow("")
    val descripcion: StateFlow<String> = _descripcion.asStateFlow()

    private val _categoria = MutableStateFlow("Otros")
    val categoria: StateFlow<String> = _categoria.asStateFlow()

    private val _latitud = MutableStateFlow(4.6682)
    val latitud: StateFlow<Double> = _latitud.asStateFlow()

    private val _longitud = MutableStateFlow(-74.0538)
    val longitud: StateFlow<Double> = _longitud.asStateFlow()

    private val _direccionAproximada = MutableStateFlow("")
    val direccionAproximada: StateFlow<String> = _direccionAproximada.asStateFlow()

    private val _analisisState = MutableStateFlow<AnalisisUiState>(AnalisisUiState.Idle)
    val analisisState: StateFlow<AnalisisUiState> = _analisisState.asStateFlow()

    private val _incidenciasSimilares = MutableStateFlow<List<Incidencia>>(emptyList())
    val incidenciasSimilares: StateFlow<List<Incidencia>> = _incidenciasSimilares.asStateFlow()

    private val _guardadoState = MutableStateFlow<GuardadoUiState>(GuardadoUiState.Idle)
    val guardadoState: StateFlow<GuardadoUiState> = _guardadoState.asStateFlow()

    private val _fotoDeGaleria = MutableStateFlow(false)
    val fotoDeGaleria: StateFlow<Boolean> = _fotoDeGaleria.asStateFlow()

    fun setFoto(bitmap: Bitmap, deGaleria: Boolean = false) {
        _fotoCaptured.value = bitmap
        _fotoDeGaleria.value = deGaleria
        _analisisState.value = AnalisisUiState.Idle
        _incidenciasSimilares.value = emptyList()
        _guardadoState.value = GuardadoUiState.Idle
        if (deGaleria) _ubicacionLista.value = false
    }

    fun setDescripcion(texto: String) {
        _descripcion.value = texto
    }

    fun setCategoria(cat: String) {
        _categoria.value = cat
    }

    private val _ubicacionLista = MutableStateFlow(false)
    val ubicacionLista: StateFlow<Boolean> = _ubicacionLista.asStateFlow()

    fun setUbicacion(lat: Double, lng: Double, direccion: String = "") {
        _latitud.value = lat
        _longitud.value = lng
        _direccionAproximada.value = direccion
        _ubicacionLista.value = true
    }

    fun analizarFotoConIA() {
        val bitmap = _fotoCaptured.value ?: return
        viewModelScope.launch {
            _analisisState.value = AnalisisUiState.Loading
            val result = geminiService.analizarFoto(bitmap)
            result.onSuccess { response ->
                _categoria.value = response.categoria
                _descripcion.value = response.descripcion
                _analisisState.value = AnalisisUiState.Success(response)
            }.onFailure { exception ->
                _analisisState.value = AnalisisUiState.Error(
                    exception.message ?: "Error al analizar imagen con Gemini AI"
                )
            }
        }
    }

    fun buscarDuplicados(radioMetros: Double = CiudadAlertaConfig.RADIO_AGRUPACION_DEFAULT_METROS) {
        viewModelScope.launch {
            try {
                val lista = incidenciasRepository.buscarSimilares(
                    latitud = _latitud.value,
                    longitud = _longitud.value,
                    categoria = _categoria.value,
                    radioMetros = radioMetros
                )
                _incidenciasSimilares.value = lista
            } catch (e: Exception) {
                _incidenciasSimilares.value = emptyList()
            }
        }
    }

    fun crearNuevoReporte(usuarioId: String, usuarioNombre: String, fotoUrl: String = "") {
        viewModelScope.launch {
            _guardadoState.value = GuardadoUiState.Loading
            try {
                var urlFinal = fotoUrl
                if (urlFinal.isBlank() && _fotoCaptured.value != null) {
                    urlFinal = incidenciasRepository.subirImagenIncidencia(_fotoCaptured.value!!)
                }

                val nuevaIncidencia = Incidencia(
                    titulo = _descripcion.value.take(50).ifBlank { "Incidencia sin título" },
                    categoria = _categoria.value,
                    ubicacion = Ubicacion(
                        latitud = _latitud.value,
                        longitud = _longitud.value,
                        direccionAproximada = _direccionAproximada.value
                    ),
                    fotoPrincipalUrl = urlFinal
                )
                val reporteInicial = Reporte(
                    usuarioId = usuarioId,
                    usuarioNombre = usuarioNombre,
                    fotoUrl = urlFinal,
                    descripcion = _descripcion.value
                )

                val idGenerado = incidenciasRepository.crearNuevaIncidencia(nuevaIncidencia, reporteInicial)
                _guardadoState.value = GuardadoUiState.Success(idGenerado)
            } catch (e: Exception) {
                _guardadoState.value = GuardadoUiState.Error(
                    e.message ?: "Error al guardar el reporte"
                )
            }
        }
    }

    fun unirAReporteExistente(incidenciaId: String, usuarioId: String, usuarioNombre: String, fotoUrl: String = "") {
        viewModelScope.launch {
            _guardadoState.value = GuardadoUiState.Loading
            try {
                var urlFinal = fotoUrl
                if (urlFinal.isBlank() && _fotoCaptured.value != null) {
                    urlFinal = incidenciasRepository.subirImagenIncidencia(_fotoCaptured.value!!)
                }

                val reporte = Reporte(
                    incidenciaId = incidenciaId,
                    usuarioId = usuarioId,
                    usuarioNombre = usuarioNombre,
                    fotoUrl = urlFinal,
                    descripcion = _descripcion.value
                )
                incidenciasRepository.unirReporteAIncidencia(incidenciaId, reporte)
                _guardadoState.value = GuardadoUiState.Success(incidenciaId)
            } catch (e: Exception) {
                _guardadoState.value = GuardadoUiState.Error(
                    e.message ?: "Error al unir reporte a incidencia existente"
                )
            }
        }
    }
}
