package com.ciudadalerta.ui.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ciudadalerta.data.model.Categoria
import com.ciudadalerta.data.model.EstadoIncidencia
import com.ciudadalerta.data.model.Incidencia
import com.ciudadalerta.data.util.GeoUtils
import com.ciudadalerta.data.util.TimeUtils
import com.ciudadalerta.ui.theme.OnSurfaceVariant
import com.ciudadalerta.ui.theme.PrimaryDark
import com.ciudadalerta.ui.theme.SurfaceContainerLow
import com.google.android.gms.maps.model.LatLng
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun NearbyListView(
    incidenciasList: List<Incidencia>,
    userLocation: LatLng?,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp)
) {
    val incidenciasOrdenadas = remember(incidenciasList, userLocation) {
        if (userLocation != null) {
            incidenciasList.sortedBy { inc ->
                GeoUtils.distanciaMetros(
                    userLocation.latitude,
                    userLocation.longitude,
                    inc.ubicacion.latitud,
                    inc.ubicacion.longitud
                )
            }
        } else {
            incidenciasList.sortedByDescending { it.fechaCreacion }
        }
    }

    if (incidenciasOrdenadas.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
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
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No hay incidencias cercanas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No encontramos reportes para la categoría o ubicación seleccionada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (userLocation == null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Ubicación no disponible. Ordenadas por fecha reciente.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(incidenciasOrdenadas, key = { it.id }) { inc ->
                val distancia = if (userLocation != null) {
                    GeoUtils.distanciaMetros(
                        userLocation.latitude,
                        userLocation.longitude,
                        inc.ubicacion.latitud,
                        inc.ubicacion.longitud
                    )
                } else null

                TarjetaIncidenciaItem(
                    incidencia = inc,
                    distanciaMetros = distancia,
                    onClick = { onNavigateToDetail(inc.id) }
                )
            }
        }
    }
}

@Composable
private fun TarjetaIncidenciaItem(
    incidencia: Incidencia,
    distanciaMetros: Double?,
    onClick: () -> Unit
) {
    val categoria = Categoria.fromString(incidencia.categoria)
    val resuelta = incidencia.estado == EstadoIncidencia.RESUELTA
    val tituloEfectivo = if (incidencia.descripcion.isNotBlank() && incidencia.descripcion.length > incidencia.titulo.length) incidencia.descripcion else incidencia.titulo
    val mostrarDescripcion = incidencia.descripcion.isNotBlank() &&
            incidencia.descripcion.trim() != tituloEfectivo.trim()

    val textoDistancia = distanciaMetros?.let { m ->
        if (m < 1000) {
            "${m.roundToInt()} m"
        } else {
            val km = m / 1000.0
            String.format(Locale.forLanguageTag("es-CO"), "%.1f km", km)
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(categoria.colorHex),
                        modifier = Modifier.size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = categoria.icono,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                    Text(
                        text = categoria.nombreCorto,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(categoria.colorHex)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (resuelta) Color(0xFFE3F2FD) else Color(0xFFFFF3E0)
                ) {
                    Text(
                        text = if (resuelta) "Resuelta" else "Activa",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (resuelta) Color(0xFF1565C0) else Color(0xFFE65100)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (incidencia.fotoPrincipalUrl.isNotBlank()) {
                    AsyncImage(
                        model = incidencia.fotoPrincipalUrl,
                        contentDescription = "Foto de la incidencia",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = categoria.icono,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = tituloEfectivo.ifBlank { "Incidencia reportada" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = incidencia.ubicacion.direccionAproximada.ifBlank { "Ubicación en el mapa" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (mostrarDescripcion) {
                        Text(
                            text = incidencia.descripcion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (textoDistancia != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryDark.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NearMe,
                                    contentDescription = null,
                                    tint = PrimaryDark,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = textoDistancia,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryDark
                                )
                            }
                        }
                    }

                    Text(
                        text = TimeUtils.tiempoTranscurrido(incidencia.fechaCreacion),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (incidencia.contadorReportes > 1) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF263238),
                        contentColor = Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReportProblem,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${incidencia.contadorReportes}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
