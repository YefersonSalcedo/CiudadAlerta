package com.ciudadalerta.ui.auth

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciudadalerta.data.model.Usuario
import com.ciudadalerta.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Success(val usuario: Usuario) : AuthUiState
    data class Error(val mensaje: String) : AuthUiState
}

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val currentUser: FirebaseUser? get() = authRepository.currentUser

    init {
        if (currentUser != null) cargarPerfil()
    }

    fun cargarPerfil() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val usuario = authRepository.crearOActualizarPerfilCiudadano()
                _uiState.value = AuthUiState.Success(usuario)
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Error al cargar perfil")
            }
        }
    }

    fun iniciarSesionConGoogle(context: Context, webClientId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                authRepository.iniciarConGoogle(context, webClientId)
                val usuario = authRepository.crearOActualizarPerfilCiudadano()
                _uiState.value = AuthUiState.Success(usuario)
                onSuccess()
            } catch (e: GetCredentialCancellationException) {
                _uiState.value = AuthUiState.Idle // el usuario cerró el selector
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Error al iniciar con Google")
            }
        }
    }

    fun cerrarSesion() {
        authRepository.cerrarSesion()
        _uiState.value = AuthUiState.Idle
    }
}
