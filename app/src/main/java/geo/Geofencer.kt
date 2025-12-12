package com.example.geofenceapplication.geo

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class Geofencer(private val context: Context) {

    private val geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    fun addGeofence(
        lat: Double,
        lng: Double,
        radiusMeters: Float,
        requestId: String
    ) {
        // Check foreground location permission
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                context,
                "No FINE_LOCATION permission, cannot add geofence",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Background location improves reliability on Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasBackground = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasBackground) {
                Toast.makeText(
                    context,
                    "No BACKGROUND_LOCATION permission – geofences may not trigger reliably.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        val geofence = Geofence.Builder()
            .setRequestId(requestId)
            .setCircularRegion(lat, lng, radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or
                        Geofence.GEOFENCE_TRANSITION_EXIT
            )
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(request, geofencePendingIntent)
            .addOnSuccessListener {
                Toast.makeText(context, "Geofence registered OK", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                val msg = if (e is ApiException) {
                    GeofenceStatusCodes.getStatusCodeString(e.statusCode)
                } else {
                    e.message ?: "Unknown error"
                }
                Toast.makeText(context, "Failed to add geofence: $msg", Toast.LENGTH_LONG).show()
            }
    }

    fun removeGeofenceByRequestId(requestId: String) {
        geofencingClient.removeGeofences(listOf(requestId))
            .addOnSuccessListener {
                Toast.makeText(context, "Geofence removed", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to remove geofence", Toast.LENGTH_SHORT).show()
            }
    }

    fun removeAllGeofences() {
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnSuccessListener {
                Toast.makeText(context, "All geofences removed", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to remove geofences", Toast.LENGTH_SHORT).show()
            }
    }
}