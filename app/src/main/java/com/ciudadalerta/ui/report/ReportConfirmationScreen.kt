package com.ciudadalerta.ui.report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ciudadalerta.ui.auth.AuthUiState
import com.ciudadalerta.ui.auth.AuthViewModel
import com.ciudadalerta.ui.theme.*

import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ReportConfirmationScreen(
    authViewModel: AuthViewModel = viewModel(),
    onViewOnMap: () -> Unit,
    onGoToMyReports: () -> Unit,
    onCreateAnother: () -> Unit
) {
    val authState by authViewModel.uiState.collectAsState()
    val puntos = when (val state = authState) {
        is AuthUiState.Success -> state.usuario.puntosCivicos
        else -> 25
    }
    val nivel = when (val state = authState) {
        is AuthUiState.Success -> state.usuario.nivel
        else -> 1
    }

    ReportConfirmationContent(
        puntos = puntos,
        nivel = nivel,
        onViewOnMap = onViewOnMap,
        onGoToMyReports = onGoToMyReports,
        onCreateAnother = onCreateAnother
    )
}

@Composable
fun ReportConfirmationContent(
    puntos: Int,
    nivel: Int,
    onViewOnMap: () -> Unit,
    onGoToMyReports: () -> Unit,
    onCreateAnother: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundLight
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(shape = CircleShape, color = TertiaryGreen, modifier = Modifier.size(80.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = OnTertiary, modifier = Modifier.size(44.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("¡Reporte publicado con éxito!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Gracias por transformar tu barrio. Ya es visible para los vecinos y cuadrillas de la zona.",
                textAlign = TextAlign.Center,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceContainerLowest,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("+25 Puntos Cívicos acumulados", color = SecondaryOrange, fontWeight = FontWeight.Bold)
                    Text("Nivel $nivel: Vecino Activo ($puntos pts)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Sigue colaborando para sumar más reconocimiento cívico en tu comunidad.", color = OutlineColor, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = onViewOnMap,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Map, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ver en el mapa", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onGoToMyReports,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Mis Reportes")
                }
                OutlinedButton(
                    onClick = onCreateAnother,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Crear otro")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReportConfirmationScreenPreview() {
    CiudadAlertaTheme {
        ReportConfirmationContent(
            puntos = 50,
            nivel = 1,
            onViewOnMap = {},
            onGoToMyReports = {},
            onCreateAnother = {}
        )
    }
}
