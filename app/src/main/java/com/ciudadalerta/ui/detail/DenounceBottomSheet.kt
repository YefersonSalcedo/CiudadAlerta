package com.ciudadalerta.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ciudadalerta.data.model.MotivoDenuncia
import com.ciudadalerta.ui.theme.*

import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DenounceBottomSheet(
    onDismiss: () -> Unit,
    onSubmit: (MotivoDenuncia, String?) -> Unit
) {
    var motivo by remember { mutableStateOf(MotivoDenuncia.INAPROPIADO) }
    var detalles by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Report, contentDescription = null, tint = PrimaryDark)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Denunciar contenido", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Text("Ayuda a mantener la comunidad cívica confiable y segura.", color = OnSurfaceVariant, style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = motivo == MotivoDenuncia.INAPROPIADO, onClick = { motivo = MotivoDenuncia.INAPROPIADO })
                Text("Contenido inapropiado u ofensivo")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = motivo == MotivoDenuncia.NO_ES_INCIDENCIA, onClick = { motivo = MotivoDenuncia.NO_ES_INCIDENCIA })
                Text("No representa una incidencia urbana")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = detalles,
                onValueChange = { detalles = it },
                label = { Text("Detalles opcionales") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onSubmit(motivo, detalles) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Enviar denuncia")
            }
        }
    }
}
