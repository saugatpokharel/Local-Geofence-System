package com.example.geofenceapplication.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
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

    private var googleMap: GoogleMap? = null
    private var marker: Marker? = null
    private var circle: Circle? = null

    // For now: fixed radius so we know it's non-zero
    private var radius = 200f  // meters

    private lateinit var geofencer: Geofencer

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        geofencer = Geofencer(requireContext())

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // 👉 Ignore SeekBar and button for now. We just want to SEE the circle.
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(gMap: GoogleMap) {
        googleMap = gMap

        gMap.uiSettings.isZoomControlsEnabled = true

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            gMap.isMyLocationEnabled = true
        }

        val defaultLatLng = LatLng(53.3498, -6.2603) // Dublin
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 12f))

        // 👉 LONG PRESS to define center
        gMap.setOnMapLongClickListener { latLng ->
            Toast.makeText(requireContext(), "Long press detected", Toast.LENGTH_SHORT).show()

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
                    .radius(radius.toDouble())   // 200m
                    .strokeWidth(6f)
                    .strokeColor(Color.RED)
                    .fillColor(0x44FF0000)       // semi-transparent red
            )
        }
    }
}
