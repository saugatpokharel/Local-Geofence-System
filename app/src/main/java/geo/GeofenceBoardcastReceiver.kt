package com.example.geofenceapplication.geo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.Geofence
import com.example.geofenceapplication.R

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        val transitionType = when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "Entered geofence"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "Exited geofence"
            else -> "Geofence trigger"
        }

        showNotification(context, transitionType)
    }

    private fun showNotification(context: Context, message: String) {
        val channelId = "geofence_channel"

        // Create notification channel (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Geofence Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val mgr = context.getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Geofence Alert")
            .setContentText(message)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(1001, notification)
    }
}
