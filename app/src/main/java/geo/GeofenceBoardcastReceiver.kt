package com.example.geofenceapplication.geo

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.geofenceapplication.R
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        // Debug: see if the receiver is actually triggered
        Toast.makeText(context, "Geofence event received", Toast.LENGTH_SHORT).show()

        val event = GeofencingEvent.fromIntent(intent)
        if (event == null) {
            Toast.makeText(context, "GeofencingEvent is null", Toast.LENGTH_SHORT).show()
            return
        }

        if (event.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(event.errorCode)
            Toast.makeText(context, "Geofence error: $errorMessage", Toast.LENGTH_LONG).show()
            return
        }

        val transitionText = when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "Entered geofence"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "Exited geofence"
            Geofence.GEOFENCE_TRANSITION_DWELL -> "Dwelling in geofence"
            else -> "Geofence trigger"
        }

        val triggeredIds = event.triggeringGeofences
            ?.joinToString { it.requestId }
            ?: "Unknown"

        val message = "$transitionText\nGeofences: $triggeredIds"

        showNotification(context, message)
    }

    private fun showNotification(context: Context, message: String) {
        val channelId = "geofence_channel"

        // Create notification channel on Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Geofence Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val mgr = context.getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(channel)
        }

        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasNotificationPermission) {
                // Permission not granted – avoid SecurityException
                Toast.makeText(
                    context,
                    "No notification permission – cannot show geofence alert.",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Geofence Alert")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(1001, builder.build())
    }
}