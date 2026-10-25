package com.ciudadalerta.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciudadalerta.data.model.Comentario
import com.ciudadalerta.data.model.Denuncia
import com.ciudadalerta.data.model.Incidencia
import com.ciudadalerta.data.repository.IncidenciasRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class IncidentDetailViewModel(
    private val incidenciasRepository: IncidenciasRepository = IncidenciasRepository()
) : ViewModel() {

    private val _incidencia = MutableStateFlow<Incidencia?>(null)
    val incidencia: StateFlow<Incidencia?> = _incidencia.asStateFlow()

    private val _comentarios = MutableStateFlow<List<Comentario>>(emptyList())
    val comentarios: StateFlow<List<Comentario>> = _comentarios.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _mensajeError = MutableStateFlow<String?>(null)
    val mensajeError: StateFlow<String?> = _mensajeError.asStateFlow()

    fun cargarIncidencia(incidenciaId: String) {
        viewModelScope.launch {
            _cargando.value = true
            _mensajeError.value = null
            try {
                val item = incidenciasRepository.obtenerIncidenciaPorId(incidenciaId)
                _incidencia.value = item
            } catch (e: Exception) {
                _mensajeError.value = e.message ?: "Error al cargar el detalle de la incidencia"
            } finally {
                _cargando.value = false
            }
        }

        incidenciasRepository.observarComentarios(incidenciaId)
            .onEach { lista ->
                _comentarios.value = lista
            }
            .launchIn(viewModelScope)
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
                incidenciasRepository.publicarComentario(nuevoComentario)
            } catch (e: Exception) {
                _mensajeError.value = e.message ?: "Error al agregar el comentario"
            }
        }
    }

    fun marcarSigueAhi(incidenciaId: String) {
        viewModelScope.launch {
            try {
                incidenciasRepository.marcarSigueAhi(incidenciaId)
                cargarIncidencia(incidenciaId)
            } catch (e: Exception) {
                _mensajeError.value = e.message ?: "Error al confirmar que sigue ahí"
            }
        }
    }

    fun marcarResuelto(incidenciaId: String, usuarioId: String) {
        viewModelScope.launch {
            try {
                val exito = incidenciasRepository.marcarResuelto(incidenciaId, usuarioId)
                if (exito) {
                    cargarIncidencia(incidenciaId)
                }
            } catch (e: Exception) {
                _mensajeError.value = e.message ?: "Error al marcar como resuelto"
            }
        }
    }

    fun denunciarIncidencia(denuncia: Denuncia) {
        viewModelScope.launch {
            try {
                incidenciasRepository.denunciarContenido(
                    contenidoId = denuncia.contenidoId,
                    usuarioId = denuncia.usuarioId,
                    motivo = denuncia.motivo,
                    detalles = denuncia.detalles
                )
            } catch (e: Exception) {
                _mensajeError.value = e.message ?: "Error al enviar la denuncia"
            }
        }
    }
}
