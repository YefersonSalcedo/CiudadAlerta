package com.ciudadalerta.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciudadalerta.data.model.Notificacion
import com.ciudadalerta.data.repository.NotificacionesRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationsViewModel(
    private val repository: NotificacionesRepository = NotificacionesRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _notificaciones = MutableStateFlow<List<Notificacion>>(emptyList())
    val notificaciones: StateFlow<List<Notificacion>> = _notificaciones.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        cargarNotificaciones()
    }

    private fun cargarNotificaciones() {
        val uid = auth.currentUser?.uid ?: run {
            _isLoading.value = false
            return
        }
        viewModelScope.launch {
            repository.observarNotificaciones(uid).collect { lista ->
                _notificaciones.value = lista
                _isLoading.value = false
            }
        }
    }

    fun marcarComoLeida(notificacionId: String) {
        val uid = auth.currentUser?.uid ?: return
        if (notificacionId.isBlank()) return
        viewModelScope.launch {
            repository.marcarComoLeida(uid, notificacionId)
        }
    }
}
