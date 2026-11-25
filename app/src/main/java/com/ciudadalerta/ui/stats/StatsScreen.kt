package com.ciudadalerta.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ciudadalerta.data.util.obtenerUbicacionActual
import com.ciudadalerta.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = viewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val loc = try { obtenerUbicacionActual(context) } catch (_: Exception) { null }
        if (loc != null) {
            viewModel.cargarEstadisticas(loc.latitude, loc.longitude)
        } else {
            viewModel.cargarEstadisticas(7.5778, -75.4044)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas Cívicas", fontWeight = FontWeight.Bold) },
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
            when (val state = uiState) {
                is StatsUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryDark)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Calculando métricas cívicas…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                    }
                }

                is StatsUiState.Empty -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SurfaceContainerLow,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = null,
                                    tint = OnSurfaceVariant,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Sin datos de estadísticas aún",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No se encontraron incidencias registradas en el sistema para calcular estadísticas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                is StatsUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Error al cargar las estadísticas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.mensaje,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.cargarEstadisticas() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reintentar")
                        }
                    }
                }

                is StatsUiState.Success -> {
                    StatsContent(
                        state = state,
                        onFiltroChanged = { soloCiudad -> viewModel.setFiltroSoloCiudad(soloCiudad) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsContent(
    state: StatsUiState.Success,
    onFiltroChanged: (Boolean) -> Unit
) {
    val stats = if (state.filtroSoloCiudad) state.statsCiudad else state.statsGlobal

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.filtroSoloCiudad,
                onClick = { onFiltroChanged(true) },
                label = { Text("En ${state.statsCiudad.ciudadNombre}") },
                leadingIcon = if (state.filtroSoloCiudad) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
            FilterChip(
                selected = !state.filtroSoloCiudad,
                onClick = { onFiltroChanged(false) },
                label = { Text("Todas las zonas") },
                leadingIcon = if (!state.filtroSoloCiudad) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }

        Text(
            text = if (state.filtroSoloCiudad)
                "Resumen de actividad e incidencias en ${stats.ciudadNombre}"
            else
                "Resumen de actividad e incidencias a nivel general",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant
        )

        // Tarjetas de Métricas Clave (Fila 1)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TarjetaMetrica(
                titulo = "Activas",
                valor = stats.totalActivas.toString(),
                icono = Icons.Default.PendingActions,
                colorIcono = Color(0xFFE65100),
                colorFondoIcono = Color(0xFFFFF3E0),
                modifier = Modifier.weight(1f)
            )

            TarjetaMetrica(
                titulo = "Resueltas",
                valor = stats.totalResueltas.toString(),
                icono = Icons.Default.CheckCircle,
                colorIcono = Color(0xFF1565C0),
                colorFondoIcono = Color(0xFFE3F2FD),
                modifier = Modifier.weight(1f)
            )
        }

        // Tarjetas de Métricas Clave (Fila 2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TarjetaMetrica(
                titulo = "Tiempo Promedio",
                valor = stats.tiempoPromedioResolucionTexto,
                icono = Icons.Default.AccessTime,
                colorIcono = PrimaryDark,
                colorFondoIcono = PrimaryFixed,
                modifier = Modifier.weight(1f)
            )

            TarjetaMetrica(
                titulo = "Más Reportada",
                valor = stats.categoriaMasReportada,
                icono = Icons.Default.Star,
                colorIcono = Color(0xFFC2185B),
                colorFondoIcono = Color(0xFFFCE4EC),
                modifier = Modifier.weight(1f)
            )
        }

        // Gráfico de Barras Horizontal por Categoría
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = PrimaryDark
                    )
                    Text(
                        text = "Incidencias por Categoría",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider()

                stats.conteoPorCategoria.forEach { catConteo ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
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
                                    color = Color(catConteo.colorHex),
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = catConteo.icono,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = catConteo.categoriaNombre,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "${catConteo.cantidad} reportes",
                                style = MaterialTheme.typography.labelMedium,
                                color = OnSurfaceVariant
                            )
                        }

                        // Barra Proporcional Horizontal
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceContainerLow)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = catConteo.proporcion.coerceIn(0.02f, 1f))
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(catConteo.colorHex))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaMetrica(
    titulo: String,
    valor: String,
    icono: ImageVector,
    colorIcono: Color,
    colorFondoIcono: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = colorFondoIcono,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icono,
                            contentDescription = null,
                            tint = colorIcono,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Text(
                text = titulo,
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant
            )

            Text(
                text = valor,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StatsScreenPreview() {
    val sample = IncidenciasStats(
        totalActivas = 12,
        totalResueltas = 8,
        tiempoPromedioResolucionTexto = "2.5 días",
        categoriaMasReportada = "Vías",
        conteoPorCategoria = emptyList(),
        ciudadNombre = "Tarazá"
    )
    CiudadAlertaTheme {
        StatsContent(
            state = StatsUiState.Success(
                statsCiudad = sample,
                statsGlobal = sample
            ),
            onFiltroChanged = {}
        )
    }
}
