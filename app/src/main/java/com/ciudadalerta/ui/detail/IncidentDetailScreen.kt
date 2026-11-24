package com.ciudadalerta.ui.detail

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ciudadalerta.data.config.CiudadAlertaConfig
import com.ciudadalerta.data.model.Comentario
import com.ciudadalerta.data.model.Denuncia
import com.ciudadalerta.data.model.EstadoIncidencia
import com.ciudadalerta.data.model.Incidencia
import com.ciudadalerta.data.model.MotivoDenuncia
import com.ciudadalerta.data.model.Reporte
import com.ciudadalerta.data.model.Ubicacion
import com.ciudadalerta.data.util.TimeUtils
import com.ciudadalerta.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

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

    val uiState by viewModel.uiState.collectAsState()

    IncidentDetailContent(
        uiState = uiState,
        onRetry = { viewModel.cargarIncidencia(incidenciaId) },
        onBack = onBack,
        onToggleFollow = { viewModel.toggleSeguirIncidencia(incidenciaId) },
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
        onResuelto = { viewModel.marcarResuelto(incidenciaId, authUser?.uid ?: "anonimo") },
        onClearError = { viewModel.limpiarMensajeError() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentDetailContent(
    uiState: IncidentDetailUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onToggleFollow: () -> Unit,
    onDenounceSubmit: (MotivoDenuncia, String?) -> Unit,
    onSendComment: (String) -> Unit,
    onSigueAhi: () -> Unit,
    onResuelto: () -> Unit,
    onClearError: () -> Unit
) {
    val context = LocalContext.current
    var showDenounceSheet by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var newCommentText by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.mensajeError) {
        val error = uiState.mensajeError
        if (error != null && uiState.incidencia != null) {
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            onClearError()
        }
    }

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(uiState.incidencia?.categoria ?: "Detalle Incidente", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Compartir") },
                                onClick = {
                                    menuExpanded = false
                                    compartirIncidencia(context, uiState.incidencia)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, contentDescription = "Compartir")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Denunciar contenido") },
                                onClick = {
                                    menuExpanded = false
                                    showDenounceSheet = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Flag, contentDescription = "Denunciar")
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceContainerLowest)
            )
        },
        bottomBar = {
            if (uiState.incidencia != null) {
                Surface(color = SurfaceContainerLowest, shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .navigationBarsPadding()
                            .imePadding(),
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
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = OnPrimary)
                        }
                    }
                }
            }
        }
    ) { padding ->
        when {
            uiState.cargando && uiState.incidencia == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Cargando detalle del incidente…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }

            uiState.mensajeError != null && uiState.incidencia == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceContainerLowest,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                uiState.mensajeError,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onRetry,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reintentar", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            uiState.incidencia != null -> {
                val inc = uiState.incidencia
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. FOTO PRINCIPAL
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        if (inc.fotoPrincipalUrl.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            ) {
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Image,
                                        contentDescription = null,
                                        tint = OutlineColor,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 2. TÍTULO Y ETIQUETA DE ESTADO
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = inc.categoria,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryDark
                                )
                                SuggestionChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            if (inc.estado == EstadoIncidencia.RESUELTA) "Resuelta" else "Activa",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (inc.estado == EstadoIncidencia.RESUELTA) TertiaryContainer else PrimaryContainer,
                                        labelColor = if (inc.estado == EstadoIncidencia.RESUELTA) OnTertiaryContainer else OnPrimaryContainer
                                    )
                                )
                            }

                            Text(
                                text = inc.titulo.ifBlank { "Incidencia reportada" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 3. DIRECCIÓN Y TIEMPO TRANSCURRIDO
                            Text(
                                text = "${inc.ubicacion.direccionAproximada.ifBlank { "Ubicación reportada" }} • ${TimeUtils.tiempoTranscurrido(inc.fechaCreacion)}",
                                color = OnSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // 4. AVISO DE 48H CON TONO SUAVE
                    if (uiState.esMiReporte && inc.necesitaReverificacion()) {
                        item {
                            Surface(
                                color = SecondaryContainer,
                                contentColor = OnSecondaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = OnSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Llevas más de 48 h sin actualizar este reporte. Por favor confirma si sigue ahí o ya se solucionó.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = OnSecondaryContainer
                                    )
                                }
                            }
                        }
                    }

                    // 5. ACCIONES COMUNITARIAS
                    item {
                        Surface(
                            color = SurfaceContainerLowest,
                            shape = RoundedCornerShape(16.dp),
                            shadowElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "¿PASASTE POR AQUÍ RECIENTEMENTE?",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = OutlineColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "${inc.contadorReportes} reportes acumulados • Resuelto: ${inc.marcasResuelto} de ${CiudadAlertaConfig.MARCAS_PARA_RESOLVER_N}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = onSigueAhi,
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                                        shape = RoundedCornerShape(14.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                    ) {
                                        Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Sigue ahí",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                    Button(
                                        onClick = onResuelto,
                                        colors = ButtonDefaults.buttonColors(containerColor = TertiaryContainer, contentColor = OnTertiaryContainer),
                                        shape = RoundedCornerShape(14.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                    ) {
                                        Icon(Icons.Default.TaskAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Ya se solucionó",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // BOTÓN DE SEGUIMIENTO (Oculto si es mi reporte)
                    if (!uiState.esMiReporte) {
                        item {
                            OutlinedButton(
                                onClick = onToggleFollow,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.siguiendo) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (uiState.siguiendo) "Siguiendo esta novedad" else "Seguir esta incidencia",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // 6. REPORTES DEL GRUPO
                    item {
                        HorizontalDivider()
                        Text(
                            "Reportes del grupo (${uiState.reportes.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (uiState.reportes.isEmpty()) {
                        item {
                            Text(
                                "Aún no hay reportes adicionales para esta incidencia.",
                                color = OnSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    } else {
                        items(uiState.reportes, key = { it.id }) { reporte ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceContainerLowest,
                                shadowElevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (reporte.fotoUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = reporte.fotoUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = reporte.usuarioNombre.ifBlank { "Ciudadano" },
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.weight(1f, fill = false),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = TimeUtils.tiempoTranscurrido(reporte.fecha),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = OutlineColor,
                                                maxLines = 1
                                            )
                                        }
                                        if (reporte.descripcion.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = reporte.descripcion,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = OnSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 7. DISCUSIÓN CIUDADANA (COMENTARIOS)
                    item {
                        HorizontalDivider()
                        Text(
                            "Discusión Ciudadana (${uiState.comentarios.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (uiState.comentarios.isEmpty()) {
                        item {
                            Text(
                                "Aún no hay comentarios. Sé el primero en aportar información.",
                                color = OnSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    } else {
                        items(uiState.comentarios, key = { it.id }) { comment ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceContainerLowest,
                                shadowElevation = 1.dp
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (!comment.usuarioFotoUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = comment.usuarioFotoUrl,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text(
                                            text = comment.usuarioNombre.ifBlank { "Ciudadano" },
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f, fill = false),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = TimeUtils.tiempoTranscurrido(comment.fecha),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = OutlineColor,
                                            maxLines = 1
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = comment.texto,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
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
            uiState = IncidentDetailUiState(
                cargando = false,
                incidencia = Incidencia(
                    id = "1",
                    titulo = "Se observa un bache de gran tamaño lleno de agua e",
                    categoria = "Vías",
                    fotoPrincipalUrl = "",
                    ubicacion = Ubicacion(latitud = 4.6682, longitud = -74.0538, direccionAproximada = "Cra. 28 # 32-4, Tarazá, Antioquia, Colombia"),
                    contadorReportes = 6,
                    marcasResuelto = 1,
                    fechaCreacion = System.currentTimeMillis() - 3600000
                ),
                comentarios = listOf(
                    Comentario(
                        id = "c1",
                        incidenciaId = "1",
                        usuarioId = "u1",
                        usuarioNombre = "Yeferson Alexis Salcedo Preciado",
                        texto = "está aún más grande",
                        fecha = System.currentTimeMillis() - 1800000
                    )
                ),
                reportes = listOf(
                    Reporte(
                        id = "r1",
                        incidenciaId = "1",
                        usuarioId = "u1",
                        usuarioNombre = "Ana Torres",
                        fotoUrl = "",
                        descripcion = "El hueco sigue aumentando.",
                        fecha = System.currentTimeMillis() - 7200000
                    )
                ),
                esMiReporte = true,
                siguiendo = false
            ),
            onRetry = {},
            onBack = {},
            onToggleFollow = {},
            onDenounceSubmit = { _, _ -> },
            onSendComment = {},
            onSigueAhi = {},
            onResuelto = {},
            onClearError = {}
        )
    }
}

private fun compartirIncidencia(context: Context, incidencia: Incidencia?) {
    if (incidencia == null) return

    val titulo = incidencia.titulo.ifBlank { "Incidencia urbana reportada" }
    val direccion = incidencia.ubicacion.direccionAproximada.ifBlank { "Ubicación en el mapa" }
    val estado = if (incidencia.estado == EstadoIncidencia.RESUELTA) "Resuelta" else "Activa"
    val lat = incidencia.ubicacion.latitud
    val lng = incidencia.ubicacion.longitud
    val urlMapa = "https://www.google.com/maps/search/?api=1&query=$lat,$lng"

    val textoCompartir = "$titulo\nDirección: $direccion\nEstado: $estado\n$urlMapa"

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, titulo)
        putExtra(Intent.EXTRA_TEXT, textoCompartir)
    }

    val chooser = Intent.createChooser(intent, "Compartir incidencia")
    context.startActivity(chooser)
}
