package com.ciudadalerta.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ciudadalerta.data.model.Categoria
import com.ciudadalerta.data.model.EstadoIncidencia
import com.ciudadalerta.data.model.Incidencia
import com.ciudadalerta.data.repository.NotificacionesRepository
import com.ciudadalerta.data.util.TimeUtils
import com.ciudadalerta.ui.components.EstadoCargando
import com.ciudadalerta.ui.components.EstadoVacio
import com.ciudadalerta.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FollowedIncidentsViewModel(
    private val notificacionesRepository: NotificacionesRepository = NotificacionesRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _cargando = MutableStateFlow(true)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    val currentUserId: String = auth.currentUser?.uid ?: ""

    val incidenciasSeguidas: StateFlow<List<Incidencia>> = if (currentUserId.isNotBlank()) {
        notificacionesRepository.observarIncidenciasSeguidas(currentUserId)
            .onEach { _cargando.value = false }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    } else {
        _cargando.value = false
        MutableStateFlow<List<Incidencia>>(emptyList()).asStateFlow()
    }

    fun dejarDeSeguir(incidenciaId: String) {
        if (currentUserId.isBlank()) return
        viewModelScope.launch {
            try {
                notificacionesRepository.dejarDeSeguir(incidenciaId, currentUserId)
            } catch (_: Exception) {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowedIncidentsScreen(
    viewModel: FollowedIncidentsViewModel = viewModel(),
    onBack: () -> Unit,
    onSelectIncident: (String) -> Unit
) {
    val incidenciasSeguidas by viewModel.incidenciasSeguidas.collectAsState()
    val cargando by viewModel.cargando.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Incidencias que sigo", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceContainerLowest)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (cargando && incidenciasSeguidas.isEmpty()) {
                EstadoCargando(
                    mensaje = "Cargando incidencias seguidas…",
                    modifier = Modifier.fillMaxSize()
                )
            } else if (incidenciasSeguidas.isEmpty()) {
                EstadoVacio(
                    icono = Icons.Default.BookmarkBorder,
                    titulo = "No sigues ninguna incidencia",
                    mensaje = "Cuando sigas incidencias para recibir notificaciones sobre su estado, aparecerán aquí.",
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(incidenciasSeguidas, key = { it.id }) { inc ->
                        TarjetaIncidenciaSeguida(
                            incidencia = inc,
                            onSelect = { onSelectIncident(inc.id) },
                            onDejarDeSeguir = { viewModel.dejarDeSeguir(inc.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaIncidenciaSeguida(
    incidencia: Incidencia,
    onSelect: () -> Unit,
    onDejarDeSeguir: () -> Unit
) {
    val categoria = Categoria.fromString(incidencia.categoria)
    val resuelta = incidencia.estado == EstadoIncidencia.RESUELTA

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(categoria.colorHex),
                        modifier = Modifier.size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = categoria.icono,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    Text(
                        text = categoria.nombre,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(categoria.colorHex)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (resuelta) Color(0xFFE3F2FD) else Color(0xFFFFF3E0)
                ) {
                    Text(
                        text = if (resuelta) "Resuelta" else "Activa",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (resuelta) Color(0xFF1565C0) else Color(0xFFE65100)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (incidencia.fotoPrincipalUrl.isNotBlank()) {
                    AsyncImage(
                        model = incidencia.fotoPrincipalUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = categoria.icono,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tituloEfectivo = if (incidencia.descripcion.isNotBlank() && incidencia.descripcion.length > incidencia.titulo.length) incidencia.descripcion else incidencia.titulo
                    Text(
                        text = tituloEfectivo.ifBlank { "Incidencia reportada" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = incidencia.ubicacion.direccionAproximada.ifBlank { "Ubicación en el mapa" },
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = TimeUtils.tiempoTranscurrido(incidencia.fechaCreacion),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onDejarDeSeguir,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkRemove,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Dejar de seguir", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
