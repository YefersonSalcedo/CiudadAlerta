package com.ciudadalerta.ui.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ciudadalerta.data.model.Categoria
import com.ciudadalerta.data.util.TimeUtils
import com.ciudadalerta.data.util.obtenerUbicacionActual
import com.ciudadalerta.ui.theme.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

import androidx.compose.ui.tooling.preview.Preview
import com.ciudadalerta.data.model.Incidencia
import com.ciudadalerta.data.model.Ubicacion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel(),
    onNavigateToNewReport: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToMyReports: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val bogota = LatLng(4.6682, -74.0538)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(bogota, 14f)
    }

    val context = LocalContext.current

    var tienePermiso by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permisoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado -> tienePermiso = resultado.values.any { it } }

    LaunchedEffect(Unit) {
        if (!tienePermiso) {
            permisoLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    LaunchedEffect(tienePermiso) {
        if (tienePermiso) {
            obtenerUbicacionActual(context)?.let { loc ->
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 16f)
                )
            }
        }
    }

    val selectedCategoryFilter by viewModel.categoriaSeleccionada.collectAsState()
    val selectedIncident by viewModel.incidenciaSeleccionada.collectAsState()
    val incidenciasList by viewModel.incidenciasFiltradas.collectAsState()

    MapScreenContent(
        cameraPositionState = cameraPositionState,
        selectedCategoryFilter = selectedCategoryFilter,
        selectedIncident = selectedIncident,
        incidenciasList = incidenciasList,
        tienePermiso = tienePermiso,
        onSelectCategory = { viewModel.seleccionarCategoria(it) },
        onSelectIncident = { viewModel.seleccionarIncidencia(it) },
        onSigueAhi = { viewModel.marcarSigueAhi(it) },
        onNavigateToNewReport = onNavigateToNewReport,
        onNavigateToDetail = onNavigateToDetail,
        onNavigateToMyReports = onNavigateToMyReports,
        onNavigateToNotifications = onNavigateToNotifications,
        onNavigateToProfile = onNavigateToProfile
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreenContent(
    cameraPositionState: CameraPositionState = rememberCameraPositionState(),
    selectedCategoryFilter: String,
    selectedIncident: Incidencia?,
    incidenciasList: List<Incidencia>,
    tienePermiso: Boolean,
    onSelectCategory: (String) -> Unit,
    onSelectIncident: (Incidencia?) -> Unit,
    onSigueAhi: (String) -> Unit,
    onNavigateToNewReport: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToMyReports: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = PrimaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.LocationCity, contentDescription = null, tint = OnPrimary)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("CiudadAlerta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryDark)
                            Text("Mapa comunitario", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Surface(shape = CircleShape, color = PrimaryFixed, modifier = Modifier.size(32.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryDark)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceContainerLowest)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = SurfaceContainerLowest) {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Default.Map, contentDescription = "Mapa") },
                    label = { Text("Mapa") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToMyReports,
                    icon = { Icon(Icons.Default.Assignment, contentDescription = "Mis reportes") },
                    label = { Text("Mis reportes") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToNotifications,
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "Notificaciones") },
                    label = { Text("Notificaciones") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToProfile,
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Perfil") },
                    label = { Text("Perfil") }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToNewReport,
                containerColor = SecondaryContainer,
                contentColor = OnPrimary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.padding(bottom = 80.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AddLocationAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reportar", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = tienePermiso),
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = true),
                onMapClick = { onSelectIncident(null) }
            ) {
                incidenciasList.forEach { inc ->
                    Marker(
                        state = rememberMarkerState(key = inc.id, position = LatLng(inc.ubicacion.latitud, inc.ubicacion.longitud)),
                        title = inc.titulo,
                        snippet = "${inc.categoria} • ${inc.contadorReportes} reportes",
                        onClick = {
                            onSelectIncident(inc)
                            true
                        }
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryFilter == "Todas",
                            onClick = { onSelectCategory("Todas") },
                            label = { Text("Todas (${incidenciasList.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryContainer,
                                selectedLabelColor = OnPrimary
                            )
                        )
                    }
                    items(Categoria.CATALOGO) { cat ->
                        FilterChip(
                            selected = selectedCategoryFilter == cat.nombre,
                            onClick = { onSelectCategory(cat.nombre) },
                            label = { Text(cat.nombre) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(cat.colorHex),
                                selectedLabelColor = OnPrimary
                            )
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = selectedIncident != null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                selectedIncident?.let { inc ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SurfaceContainerLowest,
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = SecondaryFixed,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        inc.categoria,
                                        color = OnSecondaryFixed,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    TimeUtils.tiempoTranscurrido(inc.fechaCreacion),
                                    color = OutlineColor,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (inc.fotoPrincipalUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = inc.fotoPrincipalUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                } else {
                                    Surface(
                                        color = SurfaceContainerLow,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Image, contentDescription = null, tint = OutlineColor)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(inc.titulo, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        inc.ubicacion.direccionAproximada.ifBlank { "Ubicación en mapa" },
                                        color = OnSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "${inc.contadorReportes} reportes / confirmaciones",
                                        color = SecondaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onSigueAhi(inc.id) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sigue ahí")
                                }
                                Button(
                                    onClick = { onNavigateToDetail(inc.id) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Ver detalle")
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
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
fun MapScreenPreview() {
    CiudadAlertaTheme {
        MapScreenContent(
            selectedCategoryFilter = "Todas",
            selectedIncident = Incidencia(
                id = "1",
                titulo = "Falta alumbrado público",
                categoria = "Alumbrado",
                ubicacion = Ubicacion(4.6682, -74.0538, "Carrera 15 # 85-10"),
                contadorReportes = 3,
                fechaCreacion = System.currentTimeMillis() - 7200000
            ),
            incidenciasList = listOf(
                Incidencia(
                    id = "1",
                    titulo = "Falta alumbrado público",
                    categoria = "Alumbrado",
                    ubicacion = Ubicacion(4.6682, -74.0538, "Carrera 15 # 85-10"),
                    contadorReportes = 3,
                    fechaCreacion = System.currentTimeMillis() - 7200000
                )
            ),
            tienePermiso = true,
            onSelectCategory = {},
            onSelectIncident = {},
            onSigueAhi = {},
            onNavigateToNewReport = {},
            onNavigateToDetail = {},
            onNavigateToMyReports = {},
            onNavigateToNotifications = {},
            onNavigateToProfile = {}
        )
    }
}
