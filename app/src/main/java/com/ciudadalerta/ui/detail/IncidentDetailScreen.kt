package com.ciudadalerta.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ciudadalerta.data.model.Denuncia
import com.ciudadalerta.data.util.TimeUtils
import com.ciudadalerta.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

import androidx.compose.ui.tooling.preview.Preview
import com.ciudadalerta.data.model.Comentario
import com.ciudadalerta.data.model.Incidencia
import com.ciudadalerta.data.model.MotivoDenuncia
import com.ciudadalerta.data.model.Ubicacion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentDetailScreen(
    incidenciaId: String,
    viewModel: IncidentDetailViewModel = viewModel(),
    onBack: () -> Unit
) {
    val authUser = remember { try { FirebaseAuth.getInstance().currentUser } catch (_: Exception) { null } }

    LaunchedEffect(incidenciaId) {
        viewModel.cargarIncidencia(incidenciaId)
    }

    val incidenciaDetail by viewModel.incidencia.collectAsState()
    val comentariosList by viewModel.comentarios.collectAsState()
    val cargando by viewModel.cargando.collectAsState()

    IncidentDetailContent(
        incidenciaDetail = incidenciaDetail,
        comentariosList = comentariosList,
        cargando = cargando,
        onBack = onBack,
        onDenounceSubmit = { motivo, detalles ->
            viewModel.denunciarIncidencia(
                Denuncia(
                    usuarioId = authUser?.uid ?: "anonimo",
                    contenidoId = incidenciaId,
                    motivo = motivo,
                    detalles = detalles
                )
            )
        },
        onSendComment = { texto ->
            viewModel.agregarComentario(
                incidenciaId = incidenciaId,
                usuarioId = authUser?.uid ?: "anonimo",
                usuarioNombre = authUser?.displayName ?: "Ciudadano",
                usuarioFotoUrl = authUser?.photoUrl?.toString(),
                texto = texto
            )
        },
        onSigueAhi = { viewModel.marcarSigueAhi(incidenciaId) },
        onResuelto = { viewModel.marcarResuelto(incidenciaId, authUser?.uid ?: "anonimo") }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentDetailContent(
    incidenciaDetail: Incidencia?,
    comentariosList: List<Comentario>,
    cargando: Boolean,
    onBack: () -> Unit,
    onDenounceSubmit: (MotivoDenuncia, String?) -> Unit,
    onSendComment: (String) -> Unit,
    onSigueAhi: () -> Unit,
    onResuelto: () -> Unit
) {
    var isFollowing by remember { mutableStateOf(false) }
    var showDenounceSheet by remember { mutableStateOf(false) }
    var newCommentText by remember { mutableStateOf("") }

    if (showDenounceSheet) {
        DenounceBottomSheet(
            onDismiss = { showDenounceSheet = false },
            onSubmit = { motivo, detalles ->
                onDenounceSubmit(motivo, detalles)
                showDenounceSheet = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(incidenciaDetail?.categoria ?: "Detalle Incidente", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver") }
                },
                actions = {
                    IconButton(onClick = { showDenounceSheet = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceContainerLowest)
            )
        },
        bottomBar = {
            Surface(color = SurfaceContainerLowest, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier.padding(12.dp).navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        placeholder = { Text("Escribe un comentario...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newCommentText.isNotBlank()) {
                                onSendComment(newCommentText)
                                newCommentText = ""
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = PrimaryDark)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Enviar", tint = OnPrimary)
                    }
                }
            }
        }
    ) { padding ->
        if (cargando && incidenciaDetail == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val inc = incidenciaDetail
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    if (inc != null && inc.fotoPrincipalUrl.isNotBlank()) {
                        Box(modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp))) {
                            AsyncImage(
                                model = inc.fotoPrincipalUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Surface(
                            color = SurfaceContainerLow,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(160.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = OutlineColor, modifier = Modifier.size(48.dp))
                            }
                        }
                    }
                }

                item {
                    Text(inc?.titulo ?: "Incidencia reportada", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "${inc?.ubicacion?.direccionAproximada?.ifBlank { "Ubicación reportada" } ?: ""} • ${TimeUtils.tiempoTranscurrido(inc?.fechaCreacion ?: System.currentTimeMillis())}",
                        color = OnSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                item {
                    Text("¿PASASTE POR AQUÍ RECIENTEMENTE?", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = OutlineColor)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onSigueAhi,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(52.dp)
                        ) {
                            Icon(Icons.Default.ThumbUp, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sigue ahí (${inc?.contadorReportes ?: 1})")
                        }
                        Button(
                            onClick = onResuelto,
                            colors = ButtonDefaults.buttonColors(containerColor = TertiaryContainer),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(52.dp)
                        ) {
                            Icon(Icons.Default.TaskAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ya se arregló (${inc?.marcasResuelto ?: 0})")
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { isFollowing = !isFollowing },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(if (isFollowing) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isFollowing) "Siguiendo esta novedad" else "Seguir esta incidencia")
                    }
                }

                item {
                    HorizontalDivider()
                    Text("Discusión Ciudadana (${comentariosList.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                if (comentariosList.isEmpty()) {
                    item {
                        Text(
                            "Aún no hay comentarios. Sé el primero en aportar información.",
                            color = OnSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    items(comentariosList, key = { it.id }) { comment ->
                        Surface(shape = RoundedCornerShape(12.dp), color = SurfaceContainerLowest, shadowElevation = 1.dp) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!comment.usuarioFotoUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = comment.usuarioFotoUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(24.dp).clip(CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(comment.usuarioNombre.ifBlank { "Ciudadano" }, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(TimeUtils.tiempoTranscurrido(comment.fecha), style = MaterialTheme.typography.labelSmall, color = OutlineColor)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(comment.texto, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun IncidentDetailScreenPreview() {
    CiudadAlertaTheme {
        IncidentDetailContent(
            incidenciaDetail = Incidencia(
                id = "1",
                titulo = "Hueco peligroso en la vía",
                categoria = "Malla Vial",
                fotoPrincipalUrl = "",
                ubicacion = Ubicacion(latitud = 4.6682, longitud = -74.0538, direccionAproximada = "Calle 72 # 10-34"),
                contadorReportes = 5,
                marcasResuelto = 1,
                fechaCreacion = System.currentTimeMillis() - 3600000
            ),
            comentariosList = listOf(
                Comentario(
                    id = "c1",
                    incidenciaId = "1",
                    usuarioId = "u1",
                    usuarioNombre = "Carlos Mendoza",
                    texto = "Confirmado, sigue ahí y está bastante profundo.",
                    fecha = System.currentTimeMillis() - 1800000
                )
            ),
            cargando = false,
            onBack = {},
            onDenounceSubmit = { _, _ -> },
            onSendComment = {},
            onSigueAhi = {},
            onResuelto = {}
        )
    }
}
