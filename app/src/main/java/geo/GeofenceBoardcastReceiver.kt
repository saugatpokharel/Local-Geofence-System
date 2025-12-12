package com.example.geofenceapplication.geo

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.geofenceapplication.R
import com.example.geofenceapplication.ui.GeofenceAlertActivity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        // Debug: receiver triggered
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

        val transitionType = event.geofenceTransition

        // requestId is the ID we gave when creating the geofence
        val requestId = event.triggeringGeofences
            ?.firstOrNull()
            ?.requestId
            ?: "this area"

        val userMessage = when (transitionType) {
            Geofence.GEOFENCE_TRANSITION_ENTER ->
                "You have entered the geofence: \"$requestId\""
            Geofence.GEOFENCE_TRANSITION_EXIT ->
                "You have left the geofence: \"$requestId\""
            Geofence.GEOFENCE_TRANSITION_DWELL ->
                "You are within the geofence: \"$requestId\""
            else -> "Geofence event at \"$requestId\""
        }

        // 1) Immediately open a full-screen alert with OK button
        openAlertScreen(context, userMessage)

        // 2) Optionally also show a notification (kept, but not required)
        showNotification(context, userMessage)
    }

    private fun openAlertScreen(context: Context, message: String) {
        val alertIntent = Intent(context, GeofenceAlertActivity::class.java).apply {
            putExtra("message", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(alertIntent)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
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

        val alertIntent = Intent(context, GeofenceAlertActivity::class.java).apply {
            putExtra("message", message)
        }

        val pendingAlertIntent = PendingIntent.getActivity(
            context,
            0,
            alertIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Geofence Alert")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setContentIntent(pendingAlertIntent)

        NotificationManagerCompat.from(context).notify(1001, builder.build())
    }
}