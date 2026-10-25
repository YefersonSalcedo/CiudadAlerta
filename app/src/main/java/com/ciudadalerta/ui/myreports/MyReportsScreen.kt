package com.ciudadalerta.ui.myreports

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ciudadalerta.data.model.EstadoIncidencia
import com.ciudadalerta.data.util.TimeUtils
import com.ciudadalerta.ui.theme.*

import androidx.compose.ui.tooling.preview.Preview
import com.ciudadalerta.data.model.Incidencia
import com.ciudadalerta.data.model.Ubicacion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReportsScreen(
    viewModel: MyReportsViewModel = viewModel(),
    onNavigateToMap: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onSelectReport: (String) -> Unit,
    onNewReport: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.cargarMisReportes()
    }

    val misReportesList by viewModel.misReportes.collectAsState()
    val cargando by viewModel.cargando.collectAsState()

    MyReportsContent(
        misReportesList = misReportesList,
        cargando = cargando,
        onNavigateToMap = onNavigateToMap,
        onNavigateToNotifications = onNavigateToNotifications,
        onNavigateToProfile = onNavigateToProfile,
        onSelectReport = onSelectReport,
        onNewReport = onNewReport
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReportsContent(
    misReportesList: List<Incidencia>,
    cargando: Boolean,
    onNavigateToMap: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onSelectReport: (String) -> Unit,
    onNewReport: () -> Unit
) {
    val activas = misReportesList.count { it.estado == EstadoIncidencia.ACTIVA }
    val resueltas = misReportesList.count { it.estado == EstadoIncidencia.RESUELTA }
    val apoyos = misReportesList.sumOf { it.contadorReportes }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Reportes", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceContainerLowest)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = SurfaceContainerLowest) {
                NavigationBarItem(selected = false, onClick = onNavigateToMap, icon = { Icon(Icons.Default.Map, null) }, label = { Text("Mapa") })
                NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Assignment, null) }, label = { Text("Mis reportes") })
                NavigationBarItem(selected = false, onClick = onNavigateToNotifications, icon = { Icon(Icons.Default.Notifications, null) }, label = { Text("Notificaciones") })
                NavigationBarItem(selected = false, onClick = onNavigateToProfile, icon = { Icon(Icons.Default.AccountCircle, null) }, label = { Text("Perfil") })
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewReport,
                containerColor = SecondaryContainer,
                contentColor = OnPrimary,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo reporte")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(color = SurfaceContainerLow, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceAround) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$activas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = SecondaryOrange)
                        Text("Activas")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$resueltas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TertiaryGreen)
                        Text("Resueltas")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$apoyos", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = PrimaryDark)
                        Text("Apoyos")
                    }
                }
            }

            if (cargando && misReportesList.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (misReportesList.isEmpty()) {
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
                        Icon(Icons.Default.AssignmentLate, contentDescription = null, tint = OutlineColor, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Aún no has creado ni apoyado reportes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Tus reportes y confirmaciones aparecerán aquí en tiempo real.", color = OnSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNewReport, colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark)) {
                            Icon(Icons.Default.AddLocationAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Crear primer reporte")
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(misReportesList, key = { it.id }) { inc ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SurfaceContainerLowest,
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectReport(inc.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (inc.fotoPrincipalUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = inc.fotoPrincipalUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(54.dp).clip(RoundedCornerShape(10.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                } else {
                                    Surface(color = SurfaceContainerLow, shape = RoundedCornerShape(10.dp), modifier = Modifier.size(54.dp)) {
                                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Image, contentDescription = null, tint = OutlineColor) }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(inc.titulo, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${inc.ubicacion.direccionAproximada.ifBlank { "Sin dirección" }} • ${inc.contadorReportes} reportes",
                                        color = OnSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "${inc.estado.name} • ${TimeUtils.tiempoTranscurrido(inc.fechaCreacion)}",
                                        color = if (inc.estado == EstadoIncidencia.RESUELTA) TertiaryGreen else SecondaryOrange,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = OutlineColor)
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
fun MyReportsScreenPreview() {
    CiudadAlertaTheme {
        MyReportsContent(
            misReportesList = listOf(
                Incidencia(
                    id = "1",
                    titulo = "Acumulación de basura",
                    categoria = "Aseo",
                    estado = EstadoIncidencia.ACTIVA,
                    ubicacion = Ubicacion(4.6682, -74.0538, "Calle 45 # 13-20"),
                    contadorReportes = 4,
                    fechaCreacion = System.currentTimeMillis() - 86400000
                ),
                Incidencia(
                    id = "2",
                    titulo = "Semáforo averiado",
                    categoria = "Movilidad",
                    estado = EstadoIncidencia.RESUELTA,
                    ubicacion = Ubicacion(4.6682, -74.0538, "Av. Caracas # 26-00"),
                    contadorReportes = 8,
                    fechaCreacion = System.currentTimeMillis() - 172800000
                )
            ),
            cargando = false,
            onNavigateToMap = {},
            onNavigateToNotifications = {},
            onNavigateToProfile = {},
            onSelectReport = {},
            onNewReport = {}
        )
    }
}
