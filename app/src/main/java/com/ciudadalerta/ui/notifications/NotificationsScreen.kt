package com.ciudadalerta.ui.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ciudadalerta.data.model.Notificacion
import com.ciudadalerta.data.util.TimeUtils
import com.ciudadalerta.ui.components.EstadoCargando
import com.ciudadalerta.ui.components.EstadoVacio
import com.ciudadalerta.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel = viewModel(),
    onNavigateToMap: () -> Unit,
    onNavigateToMyReports: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToDetail: ((String) -> Unit)? = null
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val context = LocalContext.current
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { _ -> }

        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val notificaciones by viewModel.notificaciones.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    NotificationsContent(
        notificaciones = notificaciones,
        isLoading = isLoading,
        onNotificationClick = { notif ->
            if (!notif.leida && notif.id.isNotBlank()) {
                viewModel.marcarComoLeida(notif.id)
            }
            if (notif.incidenciaId.isNotBlank() && onNavigateToDetail != null) {
                onNavigateToDetail(notif.incidenciaId)
            }
        },
        onNavigateToMap = onNavigateToMap,
        onNavigateToMyReports = onNavigateToMyReports,
        onNavigateToProfile = onNavigateToProfile
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsContent(
    notificaciones: List<Notificacion>,
    isLoading: Boolean = false,
    onNotificationClick: (Notificacion) -> Unit = {},
    onNavigateToMap: () -> Unit = {},
    onNavigateToMyReports: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceContainerLowest)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = SurfaceContainerLowest) {
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToMap,
                    icon = { Icon(Icons.Default.Map, null) },
                    label = { Text("Mapa", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false, fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToMyReports,
                    icon = { Icon(Icons.Default.Assignment, null) },
                    label = { Text("Reportes", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false, fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Default.Notifications, null) },
                    label = { Text("Alertas", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false, fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToProfile,
                    icon = { Icon(Icons.Default.AccountCircle, null) },
                    label = { Text("Perfil", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false, fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading) {
                EstadoCargando(mensaje = "Cargando alertas…", modifier = Modifier.padding(top = 16.dp))
            } else if (notificaciones.isEmpty()) {
                EstadoVacio(
                    icono = Icons.Default.NotificationsNone,
                    titulo = "Aún no tienes alertas",
                    mensaje = "Cuando haya novedades cerca de tu zona o solicitudes de confirmación, aparecerán aquí.",
                    modifier = Modifier.padding(top = 16.dp)
                )
            } else {
                Text(
                    "Tus Alertas",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(notificaciones, key = { it.id.ifBlank { it.hashCode().toString() } }) { notif ->
                        val cardBg = if (notif.leida) SurfaceContainerLowest else PrimaryFixed
                        Surface(
                            color = cardBg,
                            shape = RoundedCornerShape(16.dp),
                            shadowElevation = if (notif.leida) 1.dp else 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNotificationClick(notif) }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (!notif.leida) {
                                            Surface(
                                                modifier = Modifier.size(8.dp),
                                                shape = CircleShape,
                                                color = PrimaryDark
                                            ) {}
                                        }
                                        Text(
                                            if (notif.tipo == "REVERIFICACION") "ACCIÓN CÍVICA REQUERIDA" else "ACTUALIZACIÓN",
                                            color = PrimaryDark,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    Text(
                                        TimeUtils.tiempoTranscurrido(notif.fecha),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    notif.titulo,
                                    fontWeight = if (notif.leida) FontWeight.Normal else FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (notif.cuerpo.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        notif.cuerpo,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnSurfaceVariant
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
fun NotificationsScreenPreview() {
    CiudadAlertaTheme {
        NotificationsContent(
            notificaciones = listOf(
                Notificacion(
                    id = "1",
                    tipo = "REVERIFICACION",
                    incidenciaId = "inc1",
                    titulo = "Tapa de alcantarilla faltante",
                    cuerpo = "¿Pasaste por aquí? Ayuda a confirmar si el problema sigue presente.",
                    fecha = System.currentTimeMillis() - 3600000,
                    leida = false
                ),
                Notificacion(
                    id = "2",
                    tipo = "INCIDENCIA_ACTUALIZADA",
                    incidenciaId = "inc2",
                    titulo = "Semáforo averiado",
                    cuerpo = "La incidencia que sigues ha sido marcada como resuelta.",
                    fecha = System.currentTimeMillis() - 86400000,
                    leida = true
                )
            )
        )
    }
}
