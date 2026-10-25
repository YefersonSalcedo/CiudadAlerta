package com.ciudadalerta.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ciudadalerta.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ciudadalerta.R

import androidx.compose.ui.tooling.preview.Preview

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = viewModel(),
    onLoginSuccess: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val webClientId = stringResource(R.string.default_web_client_id)

    LoginScreenContent(
        uiState = uiState,
        onGoogleLogin = {
            authViewModel.iniciarSesionConGoogle(context, webClientId, onLoginSuccess)
        }
    )
}

@Composable
fun LoginScreenContent(
    uiState: AuthUiState,
    onGoogleLogin: () -> Unit
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
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceContainerLowest,
                shadowElevation = 2.dp,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.LocationCity,
                        contentDescription = null,
                        tint = PrimaryDark,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Tu voz transforma tu ciudad",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = OnSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                "Reporta, confirma y sigue incidencias en tu comunidad en tiempo real.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = OnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(40.dp))

            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Iniciando sesión segura...", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            } else {
                Button(
                    onClick = onGoogleLogin,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerLowest, contentColor = OnSurface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Continuar con Google", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            if (uiState is AuthUiState.Error) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    (uiState as AuthUiState.Error).mensaje,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                color = PrimaryFixed,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = PrimaryDark, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Acceso seguro y verificado para la comunidad ciudadana", style = MaterialTheme.typography.labelSmall, color = PrimaryDark)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                "¿Primera vez en CiudadAlerta?\nTu perfil ciudadano se creará automáticamente al ingresar",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = PrimaryDark,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    CiudadAlertaTheme {
        LoginScreenContent(
            uiState = AuthUiState.Idle,
            onGoogleLogin = {}
        )
    }
}
