package com.ciudadalerta.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ciudadalerta.ui.theme.*

@Composable
fun EstadoCargando(
    mensaje: String = "Cargando…",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(color = PrimaryDark)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = mensaje,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant
            )
        }
    }
}

@Composable
fun EstadoVacio(
    icono: ImageVector,
    titulo: String,
    mensaje: String,
    modifier: Modifier = Modifier,
    accionText: String? = null,
    onAccion: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceContainerLowest,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = SurfaceContainerLow,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icono,
                        contentDescription = null,
                        tint = OutlineColor,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = titulo,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = mensaje,
                color = OnSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            if (accionText != null && onAccion != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAccion,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(accionText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EstadoError(
    mensaje: String,
    onReintentar: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceContainerLowest,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = ErrorRed,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Ocurrió un inconveniente",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = mensaje,
                color = OnSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            if (onReintentar != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onReintentar,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reintentar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
