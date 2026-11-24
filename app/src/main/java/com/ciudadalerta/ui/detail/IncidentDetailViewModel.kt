package com.ciudadalerta.ui.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciudadalerta.data.model.Comentario
import com.ciudadalerta.data.model.Denuncia
import com.ciudadalerta.data.repository.ComentariosRepository
import com.ciudadalerta.data.repository.IncidenciasRepository
import com.ciudadalerta.data.repository.NotificacionesRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class IncidentDetailViewModel(
    private val incidenciasRepository: IncidenciasRepository = IncidenciasRepository(),
    private val comentariosRepository: ComentariosRepository = ComentariosRepository(),
    private val notificacionesRepository: NotificacionesRepository = NotificacionesRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(IncidentDetailUiState())
    val uiState: StateFlow<IncidentDetailUiState> = _uiState.asStateFlow()

    private var comentariosJob: Job? = null
    private var reportesJob: Job? = null

    val currentUserId: String? get() = auth.currentUser?.uid

    fun cargarIncidencia(incidenciaId: String) {
        comentariosJob?.cancel()
        reportesJob?.cancel()

        _uiState.update { it.copy(cargando = true, mensajeError = null) }

        viewModelScope.launch {
            try {
                val item = incidenciasRepository.obtenerIncidenciaPorId(incidenciaId)
                val uid = currentUserId
                val esPropio = if (!uid.isNullOrBlank() && item != null) {
                    item.reportantesIds.contains(uid)
                } else false

                val esSeguidor = if (!uid.isNullOrBlank() && item != null) {
                    item.seguidoresIds.contains(uid)
                } else false

                _uiState.update {
                    it.copy(
                        incidencia = item,
                        esMiReporte = esPropio,
                        siguiendo = esSeguidor,
                        cargando = false
                    )
                }
            } catch (e: Exception) {
                Log.e("IncidentDetailVM", "Error al cargar la incidencia: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        cargando = false,
                        mensajeError = e.message ?: "Error al cargar la incidencia. Intenta nuevamente."
                    )
                }
            }
        }

        comentariosJob = comentariosRepository.observarComentarios(incidenciaId)
            .onEach { lista ->
                _uiState.update { it.copy(comentarios = lista) }
            }
            .catch { e ->
                Log.e("IncidentDetailVM", "Error en stream de comentarios: ${e.message}", e)
                _uiState.update { it.copy(mensajeError = "No se pudieron actualizar los comentarios") }
            }
            .launchIn(viewModelScope)

        reportesJob = incidenciasRepository.observarReportes(incidenciaId)
            .onEach { lista ->
                _uiState.update { it.copy(reportes = lista) }
            }
            .catch { e ->
                Log.e("IncidentDetailVM", "Error en stream de reportes: ${e.message}", e)
                _uiState.update { it.copy(mensajeError = "No se pudieron cargar los reportes del grupo") }
            }
            .launchIn(viewModelScope)
    }

    fun toggleSeguirIncidencia(incidenciaId: String) {
        val uid = currentUserId ?: return
        val actualmenteSiguiendo = _uiState.value.siguiendo

        viewModelScope.launch {
            try {
                if (actualmenteSiguiendo) {
                    notificacionesRepository.dejarDeSeguir(incidenciaId, uid)
                    _uiState.update { it.copy(siguiendo = false) }
                } else {
                    notificacionesRepository.seguirIncidencia(incidenciaId, uid)
                    _uiState.update { it.copy(siguiendo = true) }
                }
            } catch (e: Exception) {
                Log.e("IncidentDetailVM", "Error al cambiar seguimiento: ${e.message}", e)
                _uiState.update { it.copy(mensajeError = "No se pudo actualizar el seguimiento") }
            }
        }
    }

    fun agregarComentario(
        incidenciaId: String,
        usuarioId: String,
        usuarioNombre: String,
        usuarioFotoUrl: String?,
        texto: String
    ) {
        if (texto.isBlank()) return
        viewModelScope.launch {
            try {
                val nuevoComentario = Comentario(
                    incidenciaId = incidenciaId,
                    usuarioId = usuarioId,
                    usuarioNombre = usuarioNombre,
                    usuarioFotoUrl = usuarioFotoUrl,
                    texto = texto.trim()
                )
                comentariosRepository.publicarComentario(nuevoComentario)
            } catch (e: Exception) {
                Log.e("IncidentDetailVM", "Error al agregar comentario: ${e.message}", e)
                _uiState.update { it.copy(mensajeError = e.message ?: "Error al publicar comentario") }
            }
        }
    }

    fun marcarSigueAhi(incidenciaId: String) {
        viewModelScope.launch {
            try {
                incidenciasRepository.marcarSigueAhi(incidenciaId)
                _uiState.update { it.copy(yaVoto = true) }
                cargarIncidencia(incidenciaId)
            } catch (e: Exception) {
                Log.e("IncidentDetailVM", "Error en marcarSigueAhi: ${e.message}", e)
                _uiState.update { it.copy(mensajeError = e.message ?: "Error al confirmar la presencia") }
            }
        }
    }

    fun marcarResuelto(incidenciaId: String, usuarioId: String) {
        viewModelScope.launch {
            try {
                val exito = incidenciasRepository.marcarResuelto(incidenciaId, usuarioId)
                if (exito) {
                    _uiState.update { it.copy(yaVoto = true) }
                    cargarIncidencia(incidenciaId)
                } else {
                    _uiState.update { it.copy(mensajeError = "Ya habías registrado una marca de resuelto") }
                }
            } catch (e: Exception) {
                Log.e("IncidentDetailVM", "Error en marcarResuelto: ${e.message}", e)
                _uiState.update { it.copy(mensajeError = e.message ?: "Error al marcar como resuelto") }
            }
        }
    }

    fun denunciarIncidencia(denuncia: Denuncia) {
        viewModelScope.launch {
            try {
                val exito = incidenciasRepository.denunciarContenido(
                    contenidoId = denuncia.contenidoId,
                    usuarioId = denuncia.usuarioId,
                    motivo = denuncia.motivo,
                    detalles = denuncia.detalles
                )
                if (exito) {
                    _uiState.update { it.copy(yaDenuncio = true) }
                } else {
                    _uiState.update { it.copy(mensajeError = "Ya habías enviado una denuncia para este contenido") }
                }
            } catch (e: Exception) {
                Log.e("IncidentDetailVM", "Error al denunciar incidencia: ${e.message}", e)
                _uiState.update { it.copy(mensajeError = e.message ?: "Error al procesar la denuncia") }
            }
        }
    }

    fun limpiarMensajeError() {
        _uiState.update { it.copy(mensajeError = null) }
    }
}
