package com.example.geofenceapplication.ui

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.geofenceapplication.R
import com.example.geofenceapplication.geo.Geofencer
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
import android.content.pm.PackageManager

class MapFragment : Fragment(R.layout.fragment_map), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null
    private var marker: Marker? = null
    private var circle: Circle? = null
    private var radius = 100f

    private lateinit var geofencer: Geofencer

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        geofencer = Geofencer(requireContext())

        // Find the map fragment
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val seekRadius = view.findViewById<SeekBar>(R.id.seekRadius)
        val btnAdd = view.findViewById<Button>(R.id.btnAddGeofence)

        seekRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                radius = progress.toFloat()
                circle?.radius = radius.toDouble()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnAdd.setOnClickListener {
            marker?.position?.let { location ->
                geofencer.addGeofence(location.latitude, location.longitude, radius)
                Toast.makeText(requireContext(), "Geofence added", Toast.LENGTH_SHORT).show()
            } ?: Toast.makeText(requireContext(), "Tap on the map first", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(gMap: GoogleMap) {
        googleMap = gMap

        gMap.uiSettings.isZoomControlsEnabled = true

        // If permission granted, enable my-location layer
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            gMap.isMyLocationEnabled = true
        }

        // Default map position (can be any location)
        val defaultLatLng = LatLng(53.3498, -6.2603) // Dublin
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 12f))

        // When user taps the map, place marker and circle
        gMap.setOnMapClickListener { latLng ->
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
                    .strokeWidth(2f)
            )
        }
    }
}
