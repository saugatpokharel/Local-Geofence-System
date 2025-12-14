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
import com.example.geofenceapplication.ui.GeofenceAlertActivity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val pendingResult = goAsync() // allow async work safely

        val event = GeofencingEvent.fromIntent(intent)
        if (event == null) {
            Toast.makeText(context, "GeofencingEvent is null", Toast.LENGTH_SHORT).show()
            pendingResult.finish()
            return
        }

        if (event.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(event.errorCode)
            Toast.makeText(context, "Geofence error: $errorMessage", Toast.LENGTH_LONG).show()
            pendingResult.finish()
            return
        }

        val transitionType = event.geofenceTransition

        val requestId = event.triggeringGeofences
            ?.firstOrNull()
            ?.requestId

        // Do DB lookup off the main thread
        CoroutineScope(Dispatchers.IO).launch {
            val friendlyName = lookupGeofenceName(context, requestId)

            val message = when (transitionType) {
                Geofence.GEOFENCE_TRANSITION_ENTER ->
                    "You have entered the geofence: \"$friendlyName\""
                Geofence.GEOFENCE_TRANSITION_EXIT ->
                    "You have left the geofence: \"$friendlyName\""
                Geofence.GEOFENCE_TRANSITION_DWELL ->
                    "You are within the geofence: \"$friendlyName\""
                else ->
                    "Geofence event at \"$friendlyName\""
            }

            // 1) Post a notification (this is what gives you sound/vibration)
            showNotificationWithSound(context, message)

            // 2) Show your dialog-style activity
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Geofence triggered: $friendlyName", Toast.LENGTH_SHORT).show()

                val alertIntent = Intent(context, GeofenceAlertActivity::class.java).apply {
                    putExtra("message", message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(alertIntent)
            }

            pendingResult.finish()
        }
    }

    private fun lookupGeofenceName(context: Context, requestId: String?): String {
        if (requestId.isNullOrBlank()) return "Unknown location"

        // requestId format: "GEOFENCE_<id>"
        val id = requestId.removePrefix("GEOFENCE_").toLongOrNull()
            ?: return requestId // fallback to raw requestId if parsing fails

        val dao = AppDatabase.getInstance(context.applicationContext).geofenceDao
        val match = dao.getAllGeofences().firstOrNull { it.id == id }

        return match?.name ?: "Saved Geofence #$id"
    }

    private fun showNotificationWithSound(context: Context, message: String) {
        val channelId = "geofence_channel"

        // Create notification channel on Android 8+ (required for sound)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(NotificationManager::class.java)

            // Create only once
            if (mgr.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(
                    channelId,
                    "Geofence Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    enableVibration(true)
                    setShowBadge(true)
                }
                mgr.createNotificationChannel(channel)
            }
        }

        // Android 13+ requires POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) return
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Geofence Alert")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setAutoCancel(true)

        NotificationManagerCompat.from(context).notify(
            System.currentTimeMillis().toInt(),
            builder.build()
        )
    }
}