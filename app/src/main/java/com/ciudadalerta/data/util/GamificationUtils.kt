package com.ciudadalerta.data.util

import com.ciudadalerta.data.config.CiudadAlertaConfig
import com.ciudadalerta.data.model.Usuario
import com.google.firebase.firestore.DocumentSnapshot
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun nivelParaPuntos(puntos: Int): Int {
    return when {
        puntos >= 400 -> 5
        puntos >= 200 -> 4
        puntos >= 90 -> 3
        puntos >= 30 -> 2
        else -> 1
    }
}

data class PuntosCalculoResult(
    val puntosCivicos: Int,
    val nivel: Int,
    val puntosInteraccionHoy: Int,
    val fechaUltimaInteraccion: String
)

fun calcularNuevosPuntos(
    usuarioSnap: DocumentSnapshot,
    puntosBase: Int,
    esInteraccion: Boolean
): PuntosCalculoResult? {
    if (!usuarioSnap.exists()) return null
    val usuario = usuarioSnap.toObject(Usuario::class.java) ?: return null
    var puntosActuales = usuario.puntosCivicos
    var puntosInteraccionHoy = usuario.puntosInteraccionHoy
    var fechaUltimaInteraccion = usuario.fechaUltimaInteraccion

    val hoyStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    if (fechaUltimaInteraccion != hoyStr) {
        puntosInteraccionHoy = 0
        fechaUltimaInteraccion = hoyStr
    }

    var puntosAOtorgar = puntosBase
    if (esInteraccion) {
        val disponibleHoy = (CiudadAlertaConfig.TOPE_PUNTOS_INTERACCION_DIA - puntosInteraccionHoy).coerceAtLeast(0)
        puntosAOtorgar = puntosBase.coerceAtMost(disponibleHoy)
        puntosInteraccionHoy += puntosAOtorgar
    }

    if (puntosAOtorgar > 0) {
        puntosActuales += puntosAOtorgar
    }
    val nuevoNivel = nivelParaPuntos(puntosActuales)

    return PuntosCalculoResult(
        puntosCivicos = puntosActuales,
        nivel = nuevoNivel,
        puntosInteraccionHoy = puntosInteraccionHoy,
        fechaUltimaInteraccion = fechaUltimaInteraccion
    )
}
