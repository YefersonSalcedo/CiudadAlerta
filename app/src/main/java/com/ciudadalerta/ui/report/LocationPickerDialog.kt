package com.ciudadalerta.ui.report

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ciudadalerta.ui.theme.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerDialog(
    inicial: LatLng,
    permisoUbicacion: Boolean,
    onConfirmar: (LatLng) -> Unit,
    onCancelar: () -> Unit
) {
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(inicial, 16f)
    }

    Dialog(
        onDismissRequest = onCancelar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("¿Dónde está el problema?") },
                        navigationIcon = {
                            IconButton(onClick = onCancelar) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar")
                            }
                        }
                    )
                },
                bottomBar = {
                    Surface(shadowElevation = 8.dp) {
                        Button(
                            onClick = { onConfirmar(cameraState.position.target) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .height(52.dp)
                        ) { Text("Confirmar ubicación") }
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraState,
                        properties = MapProperties(isMyLocationEnabled = permisoUbicacion),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            myLocationButtonEnabled = permisoUbicacion
                        )
                    )
                    // Pin fijo en el centro; su punta queda sobre el centro del mapa
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = SecondaryOrange,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(48.dp)
                            .offset(y = (-24).dp)
                    )
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(12.dp)
                    ) {
                        Text(
                            "Mueve el mapa hasta que el pin quede sobre el problema",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

