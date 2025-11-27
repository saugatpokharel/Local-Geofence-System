package com.example.geofenceapplication.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.geofenceapplication.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Request permission for ACCESS_FINE_LOCATION
    private val requestFineLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                getLastLocation()
            } else {
                toast("Location permission denied")
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val txtLocation = view.findViewById<TextView>(R.id.txtLocation)
        val btnRequestPermission = view.findViewById<Button>(R.id.btnRequestPermission)
        val btnOpenInMaps = view.findViewById<Button>(R.id.btnOpenInMaps)
        val btnShare = view.findViewById<Button>(R.id.btnShare)
        val btnOpenSensors = view.findViewById<Button>(R.id.btnOpenSensors)

        btnRequestPermission.setOnClickListener {
            requestFineLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        btnOpenInMaps.setOnClickListener {
            openLocationInMaps(txtLocation.text.toString())
        }

        btnOpenSensors.setOnClickListener {
            val intent = Intent(requireContext(), SensorActivity::class.java)
            startActivity(intent)
        }


        btnShare.setOnClickListener {
            shareApp()
        }



        getLastLocation()
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            val txtLocation = view?.findViewById<TextView>(R.id.txtLocation)

            if (location != null) {
                txtLocation?.text = "Location: ${location.latitude}, ${location.longitude}"
            } else {
                txtLocation?.text = "Location: unknown"
            }
        }
    }

    private fun openLocationInMaps(text: String) {
        val regex = Regex("(-?\\d+\\.\\d+), (-?\\d+\\.\\d+)")
        val match = regex.find(text)

        if (match != null) {
            val lat = match.groupValues[1]
            val lng = match.groupValues[2]
            val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Current Location)")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        } else {
            toast("Location unavailable")
        }
    }

    private fun shareApp() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, "Try my GeoFence application!")
        startActivity(Intent.createChooser(intent, "Share using"))
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
