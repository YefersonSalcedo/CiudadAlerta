package com.ciudadalerta.ui.report

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ciudadalerta.data.util.obtenerDireccion
import com.ciudadalerta.data.util.obtenerUbicacionActual
import com.ciudadalerta.ui.theme.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch

import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReportScreen(
    reportViewModel: ReportViewModel = viewModel(),
    onBack: () -> Unit,
    onAnalyzeWithAI: () -> Unit
) {
    val context = LocalContext.current
    val fotoCaptured by reportViewModel.fotoCaptured.collectAsState()
    val direccion by reportViewModel.direccionAproximada.collectAsState()
    val latActual by reportViewModel.latitud.collectAsState()
    val lngActual by reportViewModel.longitud.collectAsState()

    val ubicacionLista by reportViewModel.ubicacionLista.collectAsState()
    var errorUbicacion by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val fotoDeGaleria by reportViewModel.fotoDeGaleria.collectAsState()
    var mostrarSelector by remember { mutableStateOf(false) }

    val tienePermisoUbicacion = remember(ubicacionLista, mostrarSelector) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    val cargarUbicacion: () -> Unit = {
        errorUbicacion = false
        scope.launch {
            val loc = obtenerUbicacionActual(context)
            if (fotoDeGaleria) return@launch
            if (loc != null) {
                val dir = obtenerDireccion(context, loc.latitude, loc.longitude)
                if (fotoDeGaleria) return@launch
                reportViewModel.setUbicacion(loc.latitude, loc.longitude, dir)
            } else {
                errorUbicacion = true
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        if (resultado.values.any { it }) cargarUbicacion() else errorUbicacion = true
    }

    val solicitarUbicacion: () -> Unit = {
        val concedido = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

        if (concedido) {
            cargarUbicacion()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(Unit) { solicitarUbicacion() }

    val takePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            reportViewModel.setFoto(bitmap, deGaleria = false)
            solicitarUbicacion()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            takePhotoLauncher.launch(null)
        }
    }

    val launchCamera: () -> Unit = {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            takePhotoLauncher.launch(null)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap: Bitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                        val maxLado = 1600
                        val mayor = maxOf(info.size.width, info.size.height)
                        if (mayor > maxLado) {
                            val f = maxLado.toFloat() / mayor
                            decoder.setTargetSize(
                                (info.size.width * f).toInt(),
                                (info.size.height * f).toInt()
                            )
                        }
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                }
                reportViewModel.setFoto(bitmap, deGaleria = true)
                mostrarSelector = true
            } catch (_: Exception) {}
        }
    }

    CreateReportContent(
        fotoCaptured = fotoCaptured,
        direccion = direccion,
        latActual = latActual,
        lngActual = lngActual,
        ubicacionLista = ubicacionLista,
        fotoDeGaleria = fotoDeGaleria,
        errorUbicacion = errorUbicacion,
        mostrarSelector = mostrarSelector,
        tienePermisoUbicacion = tienePermisoUbicacion,
        onBack = onBack,
        onAnalyzeWithAI = onAnalyzeWithAI,
        onLaunchCamera = launchCamera,
        onPickFromGallery = { pickImageLauncher.launch("image/*") },
        onSolicitarUbicacion = solicitarUbicacion,
        onMostrarSelectorChange = { mostrarSelector = it },
        onConfirmarUbicacionManual = { punto ->
            mostrarSelector = false
            scope.launch {
                val dir = obtenerDireccion(context, punto.latitude, punto.longitude)
                reportViewModel.setUbicacion(punto.latitude, punto.longitude, dir)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReportContent(
    fotoCaptured: Bitmap?,
    direccion: String,
    latActual: Double,
    lngActual: Double,
    ubicacionLista: Boolean,
    fotoDeGaleria: Boolean,
    errorUbicacion: Boolean,
    mostrarSelector: Boolean,
    tienePermisoUbicacion: Boolean,
    onBack: () -> Unit,
    onAnalyzeWithAI: () -> Unit,
    onLaunchCamera: () -> Unit,
    onPickFromGallery: () -> Unit,
    onSolicitarUbicacion: () -> Unit,
    onMostrarSelectorChange: (Boolean) -> Unit,
    onConfirmarUbicacionManual: (LatLng) -> Unit
) {
    if (mostrarSelector) {
        LocationPickerDialog(
            inicial = LatLng(latActual, lngActual),
            permisoUbicacion = tienePermisoUbicacion,
            onConfirmar = { punto -> onConfirmarUbicacionManual(punto) },
            onCancelar = { onMostrarSelectorChange(false) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Reporte", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceContainerLowest)
            )
        },
        bottomBar = {
            Surface(color = SurfaceContainerLowest, shadowElevation = 8.dp) {
                Button(
                    onClick = onAnalyzeWithAI,
                    enabled = fotoCaptured != null && ubicacionLista,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(54.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analizar novedad con IA", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("PASO 1 DE 3", color = PrimaryDark, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                Text("Evidencia fotográfica", color = TertiaryGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
            LinearProgressIndicator(
                progress = { 0.33f },
                modifier = Modifier.fillMaxWidth(),
                color = PrimaryContainer,
                trackColor = SurfaceContainer
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                if (fotoCaptured != null) {
                    Image(
                        bitmap = fotoCaptured.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        color = OnSurface.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = TertiaryFixed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Fotografía lista para análisis", color = OnPrimary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                } else {
                    Surface(
                        color = SurfaceContainerLow,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onLaunchCamera() }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = PrimaryDark, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Toca para tomar una foto del problema", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onLaunchCamera,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tomar foto")
                }
                OutlinedButton(
                    onClick = onPickFromGallery,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Galería")
                }
            }

            Surface(
                color = SurfaceContainerLow,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, tint = PrimaryDark)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            when {
                                ubicacionLista -> direccion.ifBlank { "Ubicación seleccionada" }
                                fotoDeGaleria -> "Elige dónde está el problema"
                                errorUbicacion -> "No pudimos obtener tu ubicación"
                                else -> "Obteniendo ubicación..."
                            },
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            when {
                                fotoDeGaleria -> "Foto de galería: marca la ubicación en el mapa"
                                errorUbicacion && !ubicacionLista -> "Activa el GPS y concede el permiso de ubicación."
                                else -> "Coordenadas asignadas automáticamente"
                            },
                            color = OnSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    when {
                        fotoDeGaleria -> TextButton(onClick = { onMostrarSelectorChange(true) }) {
                            Text(if (ubicacionLista) "Cambiar" else "Elegir")
                        }
                        errorUbicacion && !ubicacionLista -> TextButton(onClick = onSolicitarUbicacion) {
                            Text("Reintentar")
                        }
                    }
                }
            }

            Surface(
                color = SurfaceContainerLowest,
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp)) {
                    Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = SecondaryOrange)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Protege la privacidad ciudadana", fontWeight = FontWeight.Bold)
                        Text(
                            "Evita incluir rostros o placas de vehículos. La IA verificará la imagen automáticamente.",
                            color = OnSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateReportScreenPreview() {
    CiudadAlertaTheme {
        CreateReportContent(
            fotoCaptured = null,
            direccion = "Calle 26 # 68D-35, Bogotá",
            latActual = 4.6682,
            lngActual = -74.0538,
            ubicacionLista = true,
            fotoDeGaleria = false,
            errorUbicacion = false,
            mostrarSelector = false,
            tienePermisoUbicacion = true,
            onBack = {},
            onAnalyzeWithAI = {},
            onLaunchCamera = {},
            onPickFromGallery = {},
            onSolicitarUbicacion = {},
            onMostrarSelectorChange = {},
            onConfirmarUbicacionManual = {}
        )
    }
}