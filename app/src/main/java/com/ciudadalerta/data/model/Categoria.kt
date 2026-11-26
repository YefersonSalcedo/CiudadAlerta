package com.ciudadalerta.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Categoria(
    val nombre: String,
    val nombreCorto: String = nombre,
    val colorHex: Long,
    val icono: ImageVector
) {
    object Vias : Categoria(
        nombre = "Vías",
        colorHex = 0xFFE65100,
        icono = Icons.Default.ReportProblem
    )

    object Alumbrado : Categoria(
        nombre = "Alumbrado público",
        nombreCorto = "Alumbrado",
        colorHex = 0xFFF57F17,
        icono = Icons.Default.Lightbulb
    )

    object Residuos : Categoria(
        nombre = "Residuos y aseo",
        nombreCorto = "Residuos",
        colorHex = 0xFF00897B,
        icono = Icons.Default.RestoreFromTrash
    )

    object Parques : Categoria(
        nombre = "Parques y zonas verdes",
        nombreCorto = "Parques",
        colorHex = 0xFF2E7D32,
        icono = Icons.Default.Park
    )

    object Semaforos : Categoria(
        nombre = "Semáforos",
        colorHex = 0xFFD32F2F,
        icono = Icons.Default.Traffic
    )

    object Arboles : Categoria(
        nombre = "Árboles",
        nombreCorto = "Árboles",
        colorHex = 0xFF5D4037,
        icono = Icons.Default.Forest
    )

    object Animales : Categoria(
        nombre = "Animales abandonados",
        nombreCorto = "Animales",
        colorHex = 0xFF7B1FA2,
        icono = Icons.Default.Pets
    )

    object Otros : Categoria(
        nombre = "Otros",
        colorHex = 0xFF546E7A,
        icono = Icons.Default.MoreHoriz
    )

    companion object {
        val CATALOGO = listOf(
            Vias,
            Alumbrado,
            Residuos,
            Parques,
            Semaforos,
            Arboles,
            Animales,
            Otros
        )

        val CATEGORIAS_BLOQUEADAS = setOf(
            "EMERGENCIA",
            "DELITO",
            "ROBO",
            "SALUD",
            "ACCIDENTE_GRAVE",
            "INCENDIO",
            "HERIDOS",
            "ARMA",
            "VIOLENCIA"
        )

        fun fromString(nombreRaw: String?): Categoria {
            if (nombreRaw.isNullOrBlank()) return Otros

            val nombre = nombreRaw.trim()

            return CATALOGO.firstOrNull {
                it.nombre.equals(nombre, ignoreCase = true) ||
                        it.nombreCorto.equals(nombre, ignoreCase = true)
            } ?: CATALOGO.firstOrNull {
                it.nombre.contains(nombre, ignoreCase = true) ||
                        nombre.contains(it.nombre, ignoreCase = true) ||
                        (nombre.contains("árbol", ignoreCase = true) && it is Arboles)
            } ?: Otros
        }
    }
}
