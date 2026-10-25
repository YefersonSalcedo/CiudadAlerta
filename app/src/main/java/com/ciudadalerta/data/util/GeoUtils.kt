package com.ciudadalerta.data.util

import kotlin.math.*

object GeoUtils {
    private const val RADIO_TIERRA_METROS = 6371000.0

    fun distanciaMetros(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2).pow(2) + cos(rLat1) * cos(rLat2) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return RADIO_TIERRA_METROS * c
    }

    fun encodeGeohash(latitude: Double, longitude: Double, precision: Int = 7): String {
        val base32 = "0123456789bcdefghjkmnpqrstuvwxyz"
        var latInterval = doubleArrayOf(-90.0, 90.0)
        var lonInterval = doubleArrayOf(-180.0, 180.0)
        val geohash = StringBuilder()
        var isEven = true
        var bit = 0
        var ch = 0

        while (geohash.length < precision) {
            if (isEven) {
                val mid = (lonInterval[0] + lonInterval[1]) / 2
                if (longitude > mid) {
                    ch = ch or (1 shl (4 - bit))
                    lonInterval[0] = mid
                } else {
                    lonInterval[1] = mid
                }
            } else {
                val mid = (latInterval[0] + latInterval[1]) / 2
                if (latitude > mid) {
                    ch = ch or (1 shl (4 - bit))
                    latInterval[0] = mid
                } else {
                    latInterval[1] = mid
                }
            }
            isEven = !isEven
            if (bit < 4) {
                bit++
            } else {
                geohash.append(base32[ch])
                bit = 0
                ch = 0
            }
        }
        return geohash.toString()
    }
}
