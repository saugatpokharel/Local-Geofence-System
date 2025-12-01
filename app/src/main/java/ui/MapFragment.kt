package com.example.geofenceapplication.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.geofenceapplication.R
import com.example.geofenceapplication.geo.Geofencer
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
class MapFragment : Fragment(R.layout.fragment_map), OnMapReadyCallback {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    private var googleMap: GoogleMap? = null
    private var marker: Marker? = null
    private var circle: Circle? = null

    private var radius = 300f
    private lateinit var geofencer: Geofencer

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        geofencer = Geofencer(requireContext())

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val seekRadius = view.findViewById<SeekBar>(R.id.seekRadius)
        seekRadius.progress = radius.toInt()

        seekRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val safeRadius = progress.coerceAtLeast(50).toFloat()
                radius = safeRadius
                circle?.radius = radius.toDouble()
            }


            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}


        })
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(gMap: GoogleMap) {
        googleMap = gMap
        gMap.uiSettings.isZoomControlsEnabled = true

        // Check permission here
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            gMap.isMyLocationEnabled = true
        } else {
            // Ask for permission directly from this Fragment
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        val defaultLatLng = LatLng(53.3498, -6.2603) // Dublin
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 15f))

        // LONG PRESS sets the geofence center AND registers geofence
        gMap.setOnMapLongClickListener { latLng ->
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

            Toast.makeText(requireContext(), "Adding geofence…", Toast.LENGTH_SHORT).show()
            geofencer.addGeofence(latLng.latitude, latLng.longitude, radius)
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
                // Now that we have permission, enable My Location
                googleMap?.let { map ->
                    if (ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        map.isMyLocationEnabled = true
                        Toast.makeText(requireContext(), "Location permission granted", Toast.LENGTH_SHORT).show()
                    }
                }
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
