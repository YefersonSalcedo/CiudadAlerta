package com.ciudadalerta.ui.profile

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ciudadalerta.ui.auth.AuthUiState
import com.ciudadalerta.ui.auth.AuthViewModel
import com.ciudadalerta.ui.theme.*

import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel = viewModel(),
    onNavigateToMap: () -> Unit,
    onNavigateToMyReports: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onLogout: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    val firebaseUser = authViewModel.currentUser

    val nombre = when (val state = uiState) {
        is AuthUiState.Success -> state.usuario.nombre
        else -> firebaseUser?.displayName ?: "Ciudadano"
    }
    val correo = when (val state = uiState) {
        is AuthUiState.Success -> state.usuario.correo
        else -> firebaseUser?.email ?: ""
    }
    val nivelPuntos = when (val state = uiState) {
        is AuthUiState.Success -> "Nivel ${state.usuario.nivel} • ${state.usuario.puntosCivicos} pts cívicos"
        else -> "Nivel 1 • 25 pts cívicos"
    }
    val fotoUrl = when (val state = uiState) {
        is AuthUiState.Success -> state.usuario.fotoUrl
        else -> firebaseUser?.photoUrl?.toString()
    }

    ProfileContent(
        nombre = nombre,
        correo = correo,
        nivelPuntos = nivelPuntos,
        fotoUrl = fotoUrl,
        onNavigateToMap = onNavigateToMap,
        onNavigateToMyReports = onNavigateToMyReports,
        onNavigateToNotifications = onNavigateToNotifications,
        onLogout = {
            authViewModel.cerrarSesion()
            onLogout()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(
    nombre: String,
    correo: String,
    nivelPuntos: String,
    fotoUrl: String?,
    onNavigateToMap: () -> Unit,
    onNavigateToMyReports: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil", fontWeight = FontWeight.Bold) },
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
                    selected = false,
                    onClick = onNavigateToNotifications,
                    icon = { Icon(Icons.Default.Notifications, null) },
                    label = { Text("Alertas", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false, fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Default.AccountCircle, null) },
                    label = { Text("Perfil", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false, fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(shape = RoundedCornerShape(16.dp), color = SurfaceContainerLowest, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!fotoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = fotoUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Surface(shape = CircleShape, color = PrimaryFixed, modifier = Modifier.size(60.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = PrimaryDark, modifier = Modifier.size(32.dp)) }
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(nombre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        if (correo.isNotBlank()) {
                            Text(correo, color = OnSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(nivelPuntos, color = PrimaryDark, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = ErrorContainer, contentColor = OnErrorContainer),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.Logout, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar sesión", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    CiudadAlertaTheme {
        ProfileContent(
            nombre = "María Alejandra Silva",
            correo = "maria.silva@ejemplo.com",
            nivelPuntos = "Nivel 3 • 120 pts cívicos",
            fotoUrl = null,
            onNavigateToMap = {},
            onNavigateToMyReports = {},
            onNavigateToNotifications = {},
            onLogout = {}
        )
    }
}
