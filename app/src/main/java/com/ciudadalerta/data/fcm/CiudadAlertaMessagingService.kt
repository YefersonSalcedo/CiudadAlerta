package com.ciudadalerta.data.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ciudadalerta.MainActivity
import com.ciudadalerta.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CiudadAlertaMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            guardarFcmToken(user.uid, token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["titulo"]
            ?: remoteMessage.data["title"]
            ?: "CiudadAlerta"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["cuerpo"]
            ?: remoteMessage.data["body"]
            ?: "Actualización comunitaria en tu zona"

        val incidenciaId = remoteMessage.data["incidenciaId"]
            ?: remoteMessage.data["id"]

        mostrarNotificacion(title, body, incidenciaId)
    }

    private fun mostrarNotificacion(title: String, message: String, incidenciaId: String?) {
        val channelId = "alertas_ciudad"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alertas de Ciudad",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de incidencias y re-verificaciones de CiudadAlerta"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            incidenciaId?.let { putExtra("incidenciaId", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    companion object {
        fun guardarFcmToken(uid: String, token: String) {
            FirebaseFirestore.getInstance().collection("usuarios").document(uid)
                .set(mapOf("fcmToken" to token), SetOptions.merge())
                .addOnFailureListener { e ->
                    Log.e("CiudadAlertaFCM", "Error al guardar fcmToken para $uid: ${e.message}", e)
                }
        }
    }
}
