package com.ciudadalerta.data.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeUtils {
    fun tiempoTranscurrido(timestampMs: Long): String {
        val diff = System.currentTimeMillis() - timestampMs
        if (diff < 0) return "Hace un momento"
        
        val segundos = diff / 1000
        val minutos = segundos / 60
        val horas = minutos / 60
        val dias = horas / 24

        return when {
            segundos < 60 -> "Hace un momento"
            minutos < 60 -> "Hace $minutos min"
            horas < 24 -> "Hace $horas h"
            dias < 7 -> "Hace $dias d"
            else -> {
                val sdf = SimpleDateFormat("d MMM yyyy", Locale("es", "CO"))
                sdf.format(Date(timestampMs))
            }
        }
    }
}
