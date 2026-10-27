package com.ciudadalerta.ui.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ciudadalerta.ui.map.MapViewModel
import com.ciudadalerta.ui.theme.*

import androidx.compose.ui.tooling.preview.Preview
import com.ciudadalerta.data.model.Incidencia
import com.ciudadalerta.data.model.Ubicacion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    mapViewModel: MapViewModel = viewModel(),
    onNavigateToMap: () -> Unit,
    onNavigateToMyReports: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val incidenciasList by mapViewModel.incidencias.collectAsState()

    NotificationsContent(
        incidenciasList = incidenciasList,
        onNavigateToMap = onNavigateToMap,
        onNavigateToMyReports = onNavigateToMyReports,
        onNavigateToProfile = onNavigateToProfile
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsContent(
    incidenciasList: List<Incidencia>,
    onNavigateToMap: () -> Unit,
    onNavigateToMyReports: () -> Unit,
    onNavigateToProfile: () -> Unit
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (incidenciasList.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceContainerLowest,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = OutlineColor, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Sin notificaciones pendientes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Cuando haya novedades cerca de tu zona o solicitudes de confirmación, aparecerán aquí.", color = OnSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                Text("Novedades activas en tu ciudad", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = PrimaryDark)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(incidenciasList.take(5), key = { it.id }) { inc ->
                        Surface(
                            color = PrimaryFixed,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("ACCIÓN CÍVICA REQUERIDA", color = PrimaryDark, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(inc.titulo, fontWeight = FontWeight.Bold)
                                Text("${inc.categoria} • ${inc.ubicacion.direccionAproximada.ifBlank { "Ubicación en mapa" }}", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("¿Pasaste por aquí? Ayuda a confirmar si el problema sigue presente.", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
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
            incidenciasList = listOf(
                Incidencia(
                    id = "1",
                    titulo = "Tapa de alcantarilla faltante",
                    categoria = "Malla Vial",
                    ubicacion = Ubicacion(4.6682, -74.0538, "Calle 100 # 19-50"),
                    contadorReportes = 2,
                    fechaCreacion = System.currentTimeMillis() - 3600000
                )
            ),
            onNavigateToMap = {},
            onNavigateToMyReports = {},
            onNavigateToProfile = {}
        )
    }
}
