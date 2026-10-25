package com.ciudadalerta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.ciudadalerta.ui.navigation.CiudadAlertaNavGraph
import com.ciudadalerta.ui.theme.CiudadAlertaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CiudadAlertaTheme {
                val navController = rememberNavController()
                CiudadAlertaNavGraph(navController = navController)
            }
        }
    }
}
