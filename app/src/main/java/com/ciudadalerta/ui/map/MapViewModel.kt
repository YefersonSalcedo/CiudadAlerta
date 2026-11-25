package com.ciudadalerta.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciudadalerta.data.model.Incidencia
import com.ciudadalerta.data.repository.IncidenciasRepository
import com.ciudadalerta.data.util.GeoUtils
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MapViewModel(
    private val incidenciasRepository: IncidenciasRepository = IncidenciasRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    companion object {
        const val MIN_REPORTES_DESTACADA = 3
        const val RADIO_ZONA_KM = 30.0
    }

    private val _centroZona = MutableStateFlow<LatLng?>(null)
    val centroZona: StateFlow<LatLng?> = _centroZona.asStateFlow()

    private val _categoriaSeleccionada = MutableStateFlow("Todas")
    val categoriaSeleccionada: StateFlow<String> = _categoriaSeleccionada.asStateFlow()

    private val _incidenciaSeleccionadaId = MutableStateFlow<String?>(null)

    private val _incidenciasConfirmadas = MutableStateFlow<Set<String>>(emptySet())
    val incidenciasConfirmadas: StateFlow<Set<String>> = _incidenciasConfirmadas.asStateFlow()

    private val _mensajeUi = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val mensajeUi: SharedFlow<String> = _mensajeUi.asSharedFlow()

    val incidencias: StateFlow<List<Incidencia>> = incidenciasRepository.observarIncidenciasActivas()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val incidenciasEnZona: StateFlow<List<Incidencia>> = combine(
        incidencias,
        _centroZona
    ) { lista, centro ->
        if (centro == null) {
            lista
        } else {
            val radioMetros = RADIO_ZONA_KM * 1000.0
            lista.filter { inc ->
                GeoUtils.distanciaMetros(
                    centro.latitude, centro.longitude,
                    inc.ubicacion.latitud, inc.ubicacion.longitud
                ) <= radioMetros
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val incidenciaSeleccionada: StateFlow<Incidencia?> = combine(
        incidenciasEnZona,
        _incidenciaSeleccionadaId,
        _categoriaSeleccionada
    ) { lista, incidenciaId, categoria ->
        lista.firstOrNull { incidencia ->
            incidencia.id == incidenciaId &&
                    (
                            categoria == "Todas" ||
                                    incidencia.categoria.equals(
                                        categoria,
                                        ignoreCase = true
                                    )
                            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val incidenciasFiltradas: StateFlow<List<Incidencia>> = combine(
        incidenciasEnZona,
        _categoriaSeleccionada
    ) { lista, categoria ->
        if (categoria == "Todas") {
            lista
        } else {
            lista.filter { it.categoria.equals(categoria, ignoreCase = true) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun establecerCentroZona(latitud: Double, longitud: Double) {
        _centroZona.value = LatLng(latitud, longitud)
        _incidenciaSeleccionadaId.value = null
    }

    fun seleccionarCategoria(categoria: String) {
        _categoriaSeleccionada.value = categoria
        _incidenciaSeleccionadaId.value = null
    }

    fun seleccionarIncidencia(incidencia: Incidencia?) {
        _incidenciaSeleccionadaId.value = incidencia?.id
    }

    fun yaMarcoSigueAhi(incidenciaId: String): Boolean {
        val uid = auth.currentUser?.uid ?: ""
        return incidenciaId in _incidenciasConfirmadas.value ||
                (uid.isNotBlank() && incidenciasRepository.yaMarcoSigueAhi(incidenciaId, uid))
    }

    fun marcarSigueAhi(incidenciaId: String) {
        val uid = auth.currentUser?.uid ?: ""
        if (yaMarcoSigueAhi(incidenciaId)) return
        viewModelScope.launch {
            try {
                incidenciasRepository.marcarSigueAhi(incidenciaId, uid)
                _incidenciasConfirmadas.update { it + incidenciaId }
                _mensajeUi.tryEmit("Confirmaste que la incidencia sigue ahí")
            } catch (_: Exception) {
                _mensajeUi.tryEmit("No se pudo registrar la confirmación")
            }
        }
    }

    fun marcarResuelto(incidenciaId: String) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            try {
                incidenciasRepository.marcarResuelto(incidenciaId, uid)
                _mensajeUi.tryEmit("Incidencia marcada como resuelta")
            } catch (_: Exception) {
                _mensajeUi.tryEmit("No se pudo marcar como resuelta")
            }
        }
    }
}
