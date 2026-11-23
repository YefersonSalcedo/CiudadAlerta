package com.ciudadalerta.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ciudadalerta.data.model.Categoria
import com.ciudadalerta.data.model.EstadoIncidencia
import com.ciudadalerta.data.model.Incidencia
import com.ciudadalerta.data.model.Ubicacion
import com.ciudadalerta.data.util.TimeUtils
import com.ciudadalerta.data.util.obtenerUbicacionActual
import com.ciudadalerta.ui.theme.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import com.google.maps.android.compose.clustering.Clustering
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val ESTILO_SIN_POIS = """
[
  { "featureType": "poi", "stylers": [{ "visibility": "off" }] },
  { "featureType": "transit", "stylers": [{ "visibility": "off" }] }
]
"""

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

    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    LaunchedEffect(tienePermiso) {
        if (tienePermiso) {
            obtenerUbicacionActual(context)?.let { loc ->
                userLocation = LatLng(loc.latitude, loc.longitude)
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 16f)
                )
            }
        }
    }

    val selectedCategoryFilter by viewModel.categoriaSeleccionada.collectAsState()
    val selectedIncident by viewModel.incidenciaSeleccionada.collectAsState()
    val incidenciasList by viewModel.incidenciasFiltradas.collectAsState()
    val allIncidencias by viewModel.incidencias.collectAsState()
    val confirmadasIds by viewModel.incidenciasConfirmadas.collectAsState()

    MapScreenContent(
        cameraPositionState = cameraPositionState,
        selectedCategoryFilter = selectedCategoryFilter,
        selectedIncident = selectedIncident,
        incidenciasList = incidenciasList,
        totalIncidenciasCount = allIncidencias.size,
        confirmadasIds = confirmadasIds,
        mensajeUi = viewModel.mensajeUi,
        tienePermiso = tienePermiso,
        userLocation = userLocation,
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

@Composable
private fun SelectorCapasMapa(
    tipoMapa: MapType,
    temaMapa: ComposeMapColorScheme,
    onTipoMapa: (MapType) -> Unit,
    onTemaMapa: (ComposeMapColorScheme) -> Unit,
    modifier: Modifier = Modifier
) {
    var abierto by remember { mutableStateOf(false) }

    val tipos = listOf(
        MapType.NORMAL to "Normal",
        MapType.SATELLITE to "Satélite"
    )
    val temas = listOf(
        ComposeMapColorScheme.LIGHT to "Claro",
        ComposeMapColorScheme.DARK to "Oscuro"
    )

    Box(modifier = modifier) {
        Surface(
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 4.dp,
            modifier = Modifier.size(40.dp)
        ) {
            IconButton(onClick = { abierto = true }, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.Layers,
                    contentDescription = "Capas del mapa",
                    tint = PrimaryDark,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            Text(
                "Tipo de mapa",
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            tipos.forEach { (tipo, nombre) ->
                DropdownMenuItem(
                    text = { Text(nombre) },
                    onClick = { onTipoMapa(tipo); abierto = false },
                    trailingIcon = {
                        if (tipo == tipoMapa) Icon(Icons.Default.Check, contentDescription = null)
                    }
                )
            }
            HorizontalDivider()
            Text(
                "Tema",
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            temas.forEach { (tema, nombre) ->
                DropdownMenuItem(
                    text = { Text(nombre) },
                    onClick = { onTemaMapa(tema); abierto = false },
                    trailingIcon = {
                        if (tema == temaMapa) Icon(Icons.Default.Check, contentDescription = null)
                    }
                )
            }
        }
    }
}

@Composable
private fun EtiquetaNav(texto: String) {
    Text(
        text = texto,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        softWrap = false,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
    )
}

@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun MapScreenContent(
    cameraPositionState: CameraPositionState = rememberCameraPositionState(),
    selectedCategoryFilter: String,
    selectedIncident: Incidencia?,
    incidenciasList: List<Incidencia>,
    totalIncidenciasCount: Int = incidenciasList.size,
    confirmadasIds: Set<String> = emptySet(),
    mensajeUi: SharedFlow<String>? = null,
    tienePermiso: Boolean,
    userLocation: LatLng? = null,
    onSelectCategory: (String) -> Unit,
    onSelectIncident: (Incidencia?) -> Unit,
    onSigueAhi: (String) -> Unit,
    onNavigateToNewReport: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToMyReports: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    var isMapView by rememberSaveable { mutableStateOf(true) }
    var tipoMapa by rememberSaveable { mutableStateOf(MapType.NORMAL) }
    var temaMapa by rememberSaveable { mutableStateOf(ComposeMapColorScheme.LIGHT) }
    var busquedaTexto by rememberSaveable { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    LaunchedEffect(mensajeUi) {
        mensajeUi?.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val clusterItems = remember(incidenciasList) {
        incidenciasList.map { IncidenciaClusterItem(it) }
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = SurfaceContainerLowest) {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Default.Map, contentDescription = "Mapa") },
                    label = { EtiquetaNav("Mapa") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToMyReports,
                    icon = { Icon(Icons.Default.Assignment, contentDescription = "Mis reportes") },
                    label = { EtiquetaNav("Mis reportes") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToNotifications,
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "Alertas") },
                    label = { EtiquetaNav("Alertas") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToProfile,
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Perfil") },
                    label = { EtiquetaNav("Perfil") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            if (isMapView) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    mapColorScheme = temaMapa,
                    contentPadding = PaddingValues(top = statusBarTop + 116.dp),
                    properties = MapProperties(
                        mapType = tipoMapa,
                        isMyLocationEnabled = tienePermiso,
                        mapStyleOptions = MapStyleOptions(ESTILO_SIN_POIS)
                    ),
                    uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
                    onMapClick = { onSelectIncident(null) }
                ) {
                    Clustering(
                        items = clusterItems,
                        onClusterClick = { cluster ->
                            scope.launch {
                                val builder = LatLngBounds.builder()
                                for (item in cluster.items) {
                                    builder.include(item.position)
                                }
                                val bounds = builder.build()
                                if (bounds.southwest == bounds.northeast) {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(
                                            bounds.southwest,
                                            cameraPositionState.position.zoom + 2f
                                        )
                                    )
                                } else {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngBounds(bounds, 100)
                                    )
                                }
                            }
                            true
                        },
                        onClusterItemClick = { item ->
                            onSelectIncident(item.incidencia)
                            true
                        },
                        clusterContent = { cluster ->
                            val categorias = cluster.items.map { it.incidencia.categoria }.distinct()
                            val colorGrupo = if (categorias.size == 1) {
                                val cat = Categoria.fromString(categorias.first())
                                Color(cat.colorHex)
                            } else {
                                PrimaryDark
                            }

                            Surface(
                                shape = CircleShape,
                                color = colorGrupo,
                                border = BorderStroke(3.dp, Color.White),
                                shadowElevation = 6.dp,
                                modifier = Modifier.size(if (cluster.size > 99) 52.dp else 44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (cluster.size > 99) "99+" else cluster.size.toString(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        },
                        clusterItemContent = { item ->
                            val inc = item.incidencia
                            val categoria = Categoria.fromString(inc.categoria)
                            val seleccionada = selectedIncident?.id == inc.id
                            val resuelta = inc.estado == EstadoIncidencia.RESUELTA
                            val mostrarContador = inc.contadorReportes > 1

                            val destacada =
                                inc.estado == EstadoIncidencia.ACTIVA &&
                                        inc.contadorReportes >= MapViewModel.MIN_REPORTES_DESTACADA

                            val colorMarcador = if (resuelta) {
                                Color(0xFF1565C0)
                            } else {
                                Color(categoria.colorHex)
                            }

                            val iconoMarcador = if (resuelta) {
                                Icons.Default.CheckCircle
                            } else {
                                categoria.icono
                            }

                            val tamanoMarcador = when {
                                seleccionada -> 54.dp
                                destacada -> 48.dp
                                else -> 40.dp
                            }

                            val colorBorde = if (destacada) {
                                Color(0xFFFFD600)
                            } else {
                                Color.White
                            }

                            Box(modifier = Modifier.padding(4.dp)) {
                                Surface(
                                    shape = CircleShape,
                                    color = colorMarcador,
                                    border = BorderStroke(
                                        width = if (destacada || seleccionada) 3.dp else 2.dp,
                                        color = colorBorde
                                    ),
                                    shadowElevation = 4.dp,
                                    modifier = Modifier
                                        .padding(
                                            top = if (mostrarContador) 8.dp else 0.dp,
                                            end = if (mostrarContador) 12.dp else 0.dp
                                        )
                                        .size(tamanoMarcador)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = iconoMarcador,
                                            contentDescription = if (resuelta) "Incidencia resuelta" else categoria.nombre,
                                            tint = Color.White,
                                            modifier = Modifier.size(if (destacada || seleccionada) 26.dp else 22.dp)
                                        )
                                    }
                                }

                                if (mostrarContador) {
                                    Surface(
                                        modifier = Modifier.align(Alignment.TopEnd),
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF263238),
                                        contentColor = Color.White,
                                        border = BorderStroke(width = 1.dp, color = Color.White)
                                    ) {
                                        Text(
                                            text = inc.contadorReportes.toString(),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            } else {
                NearbyListView(
                    incidenciasList = incidenciasList,
                    userLocation = userLocation,
                    onNavigateToDetail = onNavigateToDetail,
                    modifier = Modifier.background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(
                        top = statusBarTop + 122.dp,
                        bottom = 88.dp,
                        start = 16.dp,
                        end = 16.dp
                    )
                )
            }

            // Cabecera unificada: Buscador con logo + perfil y chips de categoría
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PrimaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LocationCity,
                                    contentDescription = "CiudadAlerta",
                                    tint = OnPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (busquedaTexto.isEmpty()) {
                                Text(
                                    text = "Buscar dirección o ciudad...",
                                    fontSize = 14.sp,
                                    color = OnSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = busquedaTexto,
                                onValueChange = { busquedaTexto = it },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 14.sp,
                                    color = Color(0xFF263238),
                                    fontWeight = FontWeight.Normal
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        val query = busquedaTexto.trim()
                                        if (query.isNotBlank()) {
                                            focusManager.clearFocus()
                                            scope.launch {
                                                val resultado = buscarUbicacionConGeocoder(context, query)
                                                resultado.onSuccess { latLng ->
                                                    cameraPositionState.animate(
                                                        CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                                                    )
                                                }.onFailure { err ->
                                                    val mensaje = if (err is NoSuchElementException) {
                                                        "No se encontraron resultados para \"$query\""
                                                    } else {
                                                        "Error al buscar la ubicación. Revisa tu conexión."
                                                    }
                                                    snackbarHostState.showSnackbar(mensaje)
                                                }
                                            }
                                        }
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (busquedaTexto.isNotEmpty()) {
                            IconButton(
                                onClick = { busquedaTexto = "" },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpiar",
                                    tint = OnSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = OnSurfaceVariant,
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onNavigateToProfile,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = PrimaryFixed,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Perfil",
                                        tint = PrimaryDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryFilter == "Todas",
                            onClick = { onSelectCategory("Todas") },
                            label = { Text("Todas ($totalIncidenciasCount)") },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White,
                                labelColor = Color(0xFF263238),
                                selectedContainerColor = PrimaryDark,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    items(Categoria.CATALOGO) { cat ->
                        FilterChip(
                            selected = selectedCategoryFilter == cat.nombre,
                            onClick = { onSelectCategory(cat.nombre) },
                            label = { Text(cat.nombreCorto) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White,
                                labelColor = Color(0xFF263238),
                                selectedContainerColor = Color(cat.colorHex),
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }

            if (isMapView) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 118.dp, end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SelectorCapasMapa(
                        tipoMapa = tipoMapa,
                        temaMapa = temaMapa,
                        onTipoMapa = { tipoMapa = it },
                        onTemaMapa = { temaMapa = it }
                    )

                    if (tienePermiso && userLocation != null) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 4.dp,
                            modifier = Modifier.size(40.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(userLocation, 16f)
                                        )
                                    }
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "Centrar en mi ubicación",
                                    tint = PrimaryDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                AnimatedVisibility(visible = !isMapView || selectedIncident == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.width(170.dp)) {
                            SegmentedButton(
                                selected = isMapView,
                                onClick = { isMapView = true },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                icon = {},
                                label = { Text("Mapa", maxLines = 1) },
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = PrimaryDark,
                                    activeContentColor = Color.White,
                                    inactiveContainerColor = Color.White,
                                    inactiveContentColor = Color(0xFF263238)
                                )
                            )
                            SegmentedButton(
                                selected = !isMapView,
                                onClick = {
                                    onSelectIncident(null)
                                    isMapView = false
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                icon = {},
                                label = { Text("Lista", maxLines = 1) },
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = PrimaryDark,
                                    activeContentColor = Color.White,
                                    inactiveContainerColor = Color.White,
                                    inactiveContentColor = Color(0xFF263238)
                                )
                            )
                        }

                        FloatingActionButton(
                            onClick = onNavigateToNewReport,
                            containerColor = SecondaryContainer,
                            contentColor = OnPrimary,
                            shape = RoundedCornerShape(18.dp)
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
                }

                AnimatedVisibility(visible = isMapView && selectedIncident != null) {
                    selectedIncident?.let { inc ->
                        key(inc.id) {
                            TarjetaIncidenciaMapa(
                                incidencia = inc,
                                yaConfirmada = inc.id in confirmadasIds,
                                onCerrar = { onSelectIncident(null) },
                                onSigueAhi = { onSigueAhi(inc.id) },
                                onVerDetalle = { onNavigateToDetail(inc.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaIncidenciaMapa(
    incidencia: Incidencia,
    yaConfirmada: Boolean,
    onCerrar: () -> Unit,
    onSigueAhi: () -> Unit,
    onVerDetalle: () -> Unit
) {
    val categoria = Categoria.fromString(incidencia.categoria)
    val resuelta = incidencia.estado == EstadoIncidencia.RESUELTA

    val mostrarDescripcion = incidencia.descripcion.isNotBlank() &&
            incidencia.descripcion.trim() != incidencia.titulo.trim()

    val personasReportantes = incidencia.reportantesIds
        .filter { it.isNotBlank() }
        .distinct()
        .size

    val textoReportantes = when (personasReportantes) {
        0 -> null
        1 -> "1 reporte"
        else -> "$personasReportantes reportes"
    }

    val textoConfirmaciones = when (incidencia.contadorConfirmaciones) {
        1 -> "1 confirmación"
        else -> "${incidencia.contadorConfirmaciones} confirmaciones"
    }

    val metadatos = listOfNotNull(
        textoReportantes,
        textoConfirmaciones,
        TimeUtils.tiempoTranscurrido(incidencia.fechaCreacion)
    ).joinToString(" • ")

    val colorEstado = if (resuelta) {
        Color(0xFF1565C0)
    } else {
        Color(0xFF9A3412)
    }

    val alturaMaxima = (LocalConfiguration.current.screenHeightDp * 0.45f).dp

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = alturaMaxima)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = categoria.icono,
                    contentDescription = null,
                    tint = Color(categoria.colorHex),
                    modifier = Modifier.size(20.dp)
                )

                Text(
                    text = categoria.nombreCorto,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = if (resuelta) "Resuelta" else "Activa",
                    color = colorEstado,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onCerrar,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar tarjeta",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (incidencia.fotoPrincipalUrl.isNotBlank()) {
                    AsyncImage(
                        model = incidencia.fotoPrincipalUrl,
                        contentDescription = "Foto de la incidencia",
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
                    Text(
                        text = incidencia.titulo.ifBlank { "Incidencia reportada" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = incidencia.ubicacion.direccionAproximada.ifBlank { "Ubicación en el mapa" },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (mostrarDescripcion) {
                Text(
                    text = incidencia.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            HorizontalDivider()

            Text(
                text = metadatos,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSigueAhi,
                    enabled = !yaConfirmada && !resuelta,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (yaConfirmada) "Confirmado" else "Sigue ahí",
                        maxLines = 1
                    )
                }

                Button(
                    onClick = onVerDetalle,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryDark,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("Ver detalle", maxLines = 1)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
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
                categoria = "Alumbrado público",
                ubicacion = Ubicacion(4.6682, -74.0538, "Carrera 15 # 85-10"),
                contadorReportes = 3,
                fechaCreacion = System.currentTimeMillis() - 7200000
            ),
            incidenciasList = listOf(
                Incidencia(
                    id = "1",
                    titulo = "Falta alumbrado público",
                    categoria = "Alumbrado público",
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

private suspend fun buscarUbicacionConGeocoder(
    context: Context,
    query: String
): Result<LatLng> = withContext(Dispatchers.IO) {
    try {
        if (query.isBlank()) return@withContext Result.failure(IllegalArgumentException("Búsqueda vacía"))
        val geocoder = Geocoder(context)
        @Suppress("DEPRECATION")
        val lista = geocoder.getFromLocationName(query, 1)
        if (!lista.isNullOrEmpty()) {
            val dir = lista[0]
            Result.success(LatLng(dir.latitude, dir.longitude))
        } else {
            Result.failure(NoSuchElementException("Sin resultados"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}