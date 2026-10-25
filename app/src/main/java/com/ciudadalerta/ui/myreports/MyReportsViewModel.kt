package com.ciudadalerta.ui.myreports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciudadalerta.data.model.Incidencia
import com.ciudadalerta.data.repository.IncidenciasRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MyReportsViewModel(
    private val incidenciasRepository: IncidenciasRepository = IncidenciasRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _misReportes = MutableStateFlow<List<Incidencia>>(emptyList())
    val misReportes: StateFlow<List<Incidencia>> = _misReportes.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private var job: Job? = null

    fun cargarMisReportes(usuarioIdOverride: String? = null) {
        val uid = if (!usuarioIdOverride.isNullOrBlank()) usuarioIdOverride else auth.currentUser?.uid
        if (uid == null) return
        _cargando.value = true
        job?.cancel()
        job = incidenciasRepository.observarIncidenciasPorUsuario(uid)
            .onEach { lista ->
                _misReportes.value = lista
                _cargando.value = false
            }
            .launchIn(viewModelScope)
    }
}
