package com.ciudadalerta.ui.report

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ciudadalerta.data.config.CiudadAlertaConfig
import com.ciudadalerta.ui.theme.*

import androidx.compose.ui.tooling.preview.Preview
import com.ciudadalerta.data.model.GeminiAnalisisResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiAnalysisScreen(
    reportViewModel: ReportViewModel = viewModel(),
    onBack: () -> Unit,
    onApproved: () -> Unit,
    onRetake: () -> Unit
) {
    val context = LocalContext.current
    val analisisState by reportViewModel.analisisState.collectAsState()

    LaunchedEffect(Unit) {
        if (analisisState is AnalisisUiState.Idle) {
            reportViewModel.analizarFotoConIA()
        }
    }

    GeminiAnalysisContent(
        analisisState = analisisState,
        onBack = onBack,
        onApproved = onApproved,
        onRetake = onRetake,
        onRetryAnalysis = { reportViewModel.analizarFotoConIA() },
        onEmergencyCall = {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${CiudadAlertaConfig.LINEA_EMERGENCIAS_123}"))
            context.startActivity(intent)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiAnalysisContent(
    analisisState: AnalisisUiState,
    onBack: () -> Unit,
    onApproved: () -> Unit,
    onRetake: () -> Unit,
    onRetryAnalysis: () -> Unit,
    onEmergencyCall: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_angle"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Análisis De Incidencia", fontWeight = FontWeight.Bold) },
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
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            when (analisisState) {
                is AnalisisUiState.Idle, is AnalisisUiState.Loading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .background(Brush.radialGradient(listOf(PrimaryFixed, Color.Transparent)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = PrimaryContainer,
                                modifier = Modifier.size(90.dp).rotate(angle)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(38.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Analizando tu foto con Gemini AI…", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Identificando categoría y validando privacidad.", textAlign = TextAlign.Center, color = OnSurfaceVariant)
                    }
                }

                is AnalisisUiState.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Error al analizar la imagen", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(analisisState.mensaje, textAlign = TextAlign.Center, color = OnSurfaceVariant)
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = onRetake) { Text("Tomar otra foto") }
                            Button(
                                onClick = onRetryAnalysis,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark)
                            ) { Text("Reintentar") }
                        }
                    }
                }

                is AnalisisUiState.Success -> {
                    val res = analisisState.resultado
                    if (res.esEmergencia || res.estado == "bloqueada") {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(shape = CircleShape, color = ErrorRed, modifier = Modifier.size(80.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Emergency, contentDescription = null, tint = OnError, modifier = Modifier.size(44.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                if (res.esEmergencia) "¡Esto parece una emergencia!" else "Fotografía no permitida",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = ErrorRed
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                res.motivoBloqueo ?: "La imagen no cumple con las políticas de privacidad o de emergencias.",
                                textAlign = TextAlign.Center,
                                color = OnSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                            if (res.esEmergencia) {
                                Button(
                                    onClick = onEmergencyCall,
                                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth().height(56.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Llamar al 123 de inmediato", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            OutlinedButton(onClick = onRetake, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                                Text("Tomar otra foto")
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(color = TertiaryFixed, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = TertiaryGreen)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Foto aprobada por IA • Categoría: ${res.categoria}", color = OnTertiaryFixed, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Surface(color = SurfaceContainerLow, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Descripción sugerida por IA:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(res.descripcion, style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Button(
                                onClick = onApproved,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                            ) {
                                Text("Continuar a verificación comunitaria", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null)
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
fun GeminiAnalysisScreenSuccessPreview() {
    CiudadAlertaTheme {
        GeminiAnalysisContent(
            analisisState = AnalisisUiState.Success(
                GeminiAnalisisResponse(
                    categoria = "Malla Vial",
                    descripcion = "Se observa un deterioro significativo sobre la carpeta asfáltica.",
                    esEmergencia = false,
                    motivoBloqueo = null,
                    estado = "aprobada"
                )
            ),
            onBack = {},
            onApproved = {},
            onRetake = {},
            onRetryAnalysis = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GeminiAnalysisScreenLoadingPreview() {
    CiudadAlertaTheme {
        GeminiAnalysisContent(
            analisisState = AnalisisUiState.Loading,
            onBack = {},
            onApproved = {},
            onRetake = {},
            onRetryAnalysis = {}
        )
    }
}
