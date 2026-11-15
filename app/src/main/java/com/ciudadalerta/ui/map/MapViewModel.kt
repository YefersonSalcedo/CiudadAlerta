package com.ciudadalerta.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciudadalerta.data.model.Incidencia
import com.ciudadalerta.data.repository.IncidenciasRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MapViewModel(
    private val incidenciasRepository: IncidenciasRepository = IncidenciasRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    companion object {
        const val MIN_REPORTES_DESTACADA = 3
    }

    private val _categoriaSeleccionada = MutableStateFlow("Todas")
    val categoriaSeleccionada: StateFlow<String> = _categoriaSeleccionada.asStateFlow()

    private val _incidenciaSeleccionadaId = MutableStateFlow<String?>(null)

    val incidencias: StateFlow<List<Incidencia>> = incidenciasRepository.observarIncidenciasActivas()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val incidenciaSeleccionada: StateFlow<Incidencia?> = combine(
        incidencias,
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
        incidencias,
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

    fun seleccionarCategoria(categoria: String) {
        _categoriaSeleccionada.value = categoria
        _incidenciaSeleccionadaId.value = null
    }

    fun seleccionarIncidencia(incidencia: Incidencia?) {
        _incidenciaSeleccionadaId.value = incidencia?.id
    }

    fun marcarSigueAhi(incidenciaId: String) {
        viewModelScope.launch {
            try {
                incidenciasRepository.marcarSigueAhi(incidenciaId)
            } catch (_: Exception) {}
        }
    }

    fun marcarResuelto(incidenciaId: String) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            try {
                incidenciasRepository.marcarResuelto(incidenciaId, uid)
            } catch (_: Exception) {}
        }
    }
}
