package com.example.geofenceapplication.geo

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class Geofencer(private val context: Context) {

    // Client that talks to Google Play Services location / geofencing
    private val geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(context)

    // PendingIntent that will deliver geofence transitions to GeofenceBroadcastReceiver
    private val geofencePendingIntent: PendingIntent by lazy {
        // IMPORTANT: explicit intent, no custom action
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)

        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }


    /**
     * Registers a single circular geofence at (lat, lng) with the given radius in meters.
     * Uses GPS/location sensor under the hood via Google Play Services.
     */
    fun addGeofence(
        lat: Double,
        lng: Double,
        radiusMeters: Float
    ) {
        // Safety: ensure location permission is granted before proceeding
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // No permission -> don't crash, just skip adding geofence
            return
        }

        // Define one geofence
        val geofence = Geofence.Builder()
            .setRequestId("GEOFENCE_${System.currentTimeMillis()}") // unique id
            .setCircularRegion(lat, lng, radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or
                        Geofence.GEOFENCE_TRANSITION_EXIT
            )
            .build()

        // Wrap it into a request
        val request = GeofencingRequest.Builder()

            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        // Finally, register with the system
        geofencingClient.addGeofences(request, geofencePendingIntent)
            .addOnSuccessListener {

                // Log.d("Geofencer", "Geofence added successfully")
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                // Optionally log or handle error
            }
    }
}
