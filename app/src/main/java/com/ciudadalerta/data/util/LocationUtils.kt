package com.ciudadalerta.data.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

@SuppressLint("MissingPermission")
suspend fun obtenerUbicacionActual(context: Context): Location? {
    val client = LocationServices.getFusedLocationProviderClient(context)
    return try {
        client.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).await()
    } catch (e: Exception) {
        null
    }
}

@Suppress("DEPRECATION")
suspend fun obtenerDireccion(context: Context, lat: Double, lng: Double): String =
    withContext(Dispatchers.IO) {
        try {
            Geocoder(context, Locale("es", "CO"))
                .getFromLocation(lat, lng, 1)
                ?.firstOrNull()
                ?.getAddressLine(0)
                .orEmpty()
        } catch (e: Exception) {
            ""
        }
    }