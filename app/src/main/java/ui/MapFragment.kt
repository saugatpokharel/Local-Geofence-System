package com.example.geofenceapplication.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.geofenceapplication.R
import com.example.geofenceapplication.geo.Geofencer
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import data.AppDatabase
import data.GeofenceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MapFragment : Fragment(R.layout.fragment_map), OnMapReadyCallback {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    private var googleMap: GoogleMap? = null
    private var marker: Marker? = null
    private var circle: Circle? = null

    private var radius = 300f
    private lateinit var geofencer: Geofencer
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        geofencer = Geofencer(requireContext())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val seekRadius = view.findViewById<SeekBar>(R.id.seekRadius)
        seekRadius.progress = radius.toInt()

        seekRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                val safeRadius = progress.coerceAtLeast(50).toFloat()
                radius = safeRadius
                circle?.radius = radius.toDouble()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    /**
     * Save a geofence definition persistently using our simple "database"
     * (SharedPreferences + JSON behind the scenes).
     */
    private fun saveGeofenceToDb(latLng: LatLng, radiusMeters: Float, name: String) {
        val dao = AppDatabase.getInstance(requireContext()).geofenceDao

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            dao.upsertGeofence(
                GeofenceEntity(
                    name = name,
                    latitude = latLng.latitude,
                    longitude = latLng.longitude,
                    radiusMeters = radiusMeters
                )
            )
        }
    }

    /**
     * Show a dialog so the user can give a friendly name to the geofence.
     * Only after they confirm do we register + save the geofence.
     */
    private fun showNameInputDialog(latLng: LatLng) {
        val context = requireContext()
        val input = EditText(context).apply {
            hint = "e.g. Home, Work, College"
        }

        AlertDialog.Builder(context)
            .setTitle("Name this geofence")
            .setView(input)
            .setPositiveButton("Save") { dialog, _ ->
                val typedName = input.text.toString().trim()
                val finalName = if (typedName.isNotEmpty()) {
                    typedName
                } else {
                    "Unnamed Geofence"
                }

                viewLifecycleOwner.lifecycleScope.launch {

                    // 1️⃣ Save to DB FIRST (this gives us a real ID)
                    val saved = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        val dao = AppDatabase.getInstance(requireContext()).geofenceDao
                        dao.upsertGeofence(
                            GeofenceEntity(
                                name = finalName,
                                latitude = latLng.latitude,
                                longitude = latLng.longitude,
                                radiusMeters = radius
                            )
                        )
                    }

                    // 2️⃣ Register geofence with STABLE requestId
                    val requestId = "GEOFENCE_${saved.id}"
                    geofencer.addGeofence(
                        lat = saved.latitude,
                        lng = saved.longitude,
                        radiusMeters = saved.radiusMeters,
                        requestId = requestId
                    )

                    Toast.makeText(
                        context,
                        "Geofence \"${saved.name}\" saved",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    /**
     * Enable the blue "my location" dot and center the camera on the
     * device's last known location, if available. If not, fall back to Dublin.
     */
    @SuppressLint("MissingPermission")
    private fun enableMyLocationAndCenter() {
        val map = googleMap ?: return

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        map.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                currentLatLng,
                                15f
                            )
                        )
                    } else {
                        // No last known location: fallback to Dublin
                        val dublin = LatLng(53.3498, -6.2603)
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(dublin, 15f))
                    }
                }
                .addOnFailureListener {
                    // On error, just fallback to Dublin
                    val dublin = LatLng(53.3498, -6.2603)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(dublin, 15f))
                }
        } else {
            // No permission, move to Dublin and ask for it
            val dublin = LatLng(53.3498, -6.2603)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(dublin, 15f))
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(gMap: GoogleMap) {
        googleMap = gMap
        gMap.uiSettings.isZoomControlsEnabled = true

        // Check location permission
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocationAndCenter()
        } else {
            // Ask for permission directly from this Fragment
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )

            // Until permission is granted, just show Dublin
            val dublin = LatLng(53.3498, -6.2603)
            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(dublin, 15f))
        }

        // LONG PRESS sets the geofence center and shows a naming dialog
        gMap.setOnMapLongClickListener { latLng ->
            // Update marker + circle
            marker?.remove()
            circle?.remove()

            marker = gMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Geofence Center")
            )

            circle = gMap.addCircle(
                CircleOptions()
                    .center(latLng)
                    .radius(radius.toDouble())
                    .strokeWidth(6f)
                    .strokeColor(Color.RED)
                    .fillColor(0x44FF0000)
            )

            // Ask the user for a friendly name before saving
            showNameInputDialog(latLng)
        }
    }

    // Handle the permission result here
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                // Now that we have permission, enable My Location and center
                enableMyLocationAndCenter()

                Toast.makeText(
                    requireContext(),
                    "Location permission granted",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Location permission denied – geofencing may not work",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}