package com.ciudadalerta.ui.report

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.ciudadalerta.data.util.TimeUtils
import com.ciudadalerta.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

import androidx.compose.ui.tooling.preview.Preview
import com.ciudadalerta.data.model.Incidencia
import com.ciudadalerta.data.model.Ubicacion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateDetectionScreen(
    reportViewModel: ReportViewModel = viewModel(),
    onBack: () -> Unit,
    onMatchConfirmed: (String) -> Unit,
) {
    val authUser = remember { try { FirebaseAuth.getInstance().currentUser } catch (_: Exception) { null } }
    val incidenciasSimilares by reportViewModel.incidenciasSimilares.collectAsState()
    val guardadoState by reportViewModel.guardadoState.collectAsState()

    LaunchedEffect(Unit) {
        reportViewModel.buscarDuplicados()
    }

    LaunchedEffect(guardadoState) {
        if (guardadoState is GuardadoUiState.Success) {
            val id = (guardadoState as GuardadoUiState.Success).incidenciaId
            onMatchConfirmed(id)
        }
    }

    val uid = authUser?.uid ?: "anonimo"
    val uNombre = authUser?.displayName ?: "Ciudadano"

    DuplicateDetectionContent(
        incidenciasSimilares = incidenciasSimilares,
        guardadoState = guardadoState,
        onBack = onBack,
        onUnirAReporte = { id -> reportViewModel.unirAReporteExistente(id, uid, uNombre) },
        onCrearNuevoReporte = { reportViewModel.crearNuevoReporte(uid, uNombre) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateDetectionContent(
    incidenciasSimilares: List<Incidencia>,
    guardadoState: GuardadoUiState,
    onBack: () -> Unit,
    onUnirAReporte: (String) -> Unit,
    onCrearNuevoReporte: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear Reporte", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceContainerLowest)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Text("PASO 2 DE 3 • VERIFICACIÓN COMUNITARIA", color = PrimaryDark, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            LinearProgressIndicator(progress = { 0.66f }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), color = PrimaryContainer)

            Text("¿Es el mismo problema?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Detectamos si existen incidencias similares registradas en la zona para evitar duplicados.", color = OnSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            if (guardadoState is GuardadoUiState.Error) {
                Text(
                    guardadoState.mensaje,
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (guardadoState is GuardadoUiState.Loading) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Publicando tu reporte en la comunidad…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else if (incidenciasSimilares.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceContainerLowest,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = TertiaryGreen, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Sin duplicados cercanos", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("No hay otros reportes recientes con esta misma categoría en esta ubicación.", color = OnSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(incidenciasSimilares, key = { it.id }) { inc ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SurfaceContainerLowest,
                            shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Surface(color = SecondaryFixed, shape = RoundedCornerShape(8.dp)) {
                                        Text(inc.categoria, fontWeight = FontWeight.Bold, color = OnSecondaryFixed, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                    }
                                    Text(TimeUtils.tiempoTranscurrido(inc.fechaCreacion), color = SecondaryContainer, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (inc.fotoPrincipalUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = inc.fotoPrincipalUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(70.dp).clip(RoundedCornerShape(10.dp))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(inc.titulo, fontWeight = FontWeight.Bold)
                                        Text(inc.ubicacion.direccionAproximada.ifBlank { "En tu zona" }, color = OnSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                        Text("${inc.contadorReportes} reportes acumulados", color = TertiaryGreen, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { onUnirAReporte(inc.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.HowToReg, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sí, es este problema (sumar mi apoyo)")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onCrearNuevoReporte,
                enabled = guardadoState !is GuardadoUiState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerHigh, contentColor = PrimaryDark),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.AddLocationAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Publicar como nuevo reporte", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DuplicateDetectionScreenPreview() {
    CiudadAlertaTheme {
        DuplicateDetectionContent(
            incidenciasSimilares = listOf(
                Incidencia(
                    id = "1",
                    titulo = "Fuga de agua potable",
                    categoria = "Servicios Públicos",
                    ubicacion = Ubicacion(4.6682, -74.0538, "Carrera 7 # 45-10"),
                    contadorReportes = 3,
                    fechaCreacion = System.currentTimeMillis() - 7200000
                )
            ),
            guardadoState = GuardadoUiState.Idle,
            onBack = {},
            onUnirAReporte = {},
            onCrearNuevoReporte = {}
        )
    }
}
