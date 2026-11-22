package com.ciudadalerta

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.ciudadalerta.ui.navigation.CiudadAlertaNavGraph
import com.ciudadalerta.ui.theme.CiudadAlertaTheme

class MainActivity : ComponentActivity() {

    private var incidenciaIdInicial by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incidenciaIdInicial = intent?.getStringExtra("incidenciaId")
        enableEdgeToEdge()
        setContent {
            CiudadAlertaTheme {
                val navController = rememberNavController()
                CiudadAlertaNavGraph(
                    navController = navController,
                    incidenciaIdInicial = incidenciaIdInicial,
                    onIncidenciaConsumida = { incidenciaIdInicial = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incidenciaIdInicial = intent.getStringExtra("incidenciaId")
    }
}
