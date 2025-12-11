package com.example.geofenceapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.geofenceapplication.geo.Geofencer
import com.google.android.material.bottomnavigation.BottomNavigationView
import data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private val REQUIRED_PERMISSIONS by lazy {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            // Background location for Android 10+ (Q)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            // Notification permission for Android 13+ (Tiramisu)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    private val PERMISSION_REQUEST_CODE = 1234

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setupWithNavController(navController)

        // Ask for permissions early
        requestAllPermissionsIfNeeded()

        // If already granted (e.g. returning to app), re-register geofences now
        if (hasAllPermissions()) {
            reRegisterGeofencesFromDb()
        }
    }

    private fun hasAllPermissions(): Boolean =
        REQUIRED_PERMISSIONS.all { perm ->
            ContextCompat.checkSelfPermission(this, perm) ==
                    PackageManager.PERMISSION_GRANTED
        }

    private fun requestAllPermissionsIfNeeded() {
        if (!hasAllPermissions()) {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            val grantedCount = grantResults.count { it == PackageManager.PERMISSION_GRANTED }
            val total = permissions.size

            if (grantedCount == total) {
                Toast.makeText(this, "All permissions granted ✅", Toast.LENGTH_SHORT).show()
                // Now that we have permissions, (re)register geofences
                reRegisterGeofencesFromDb()
            } else {
                Toast.makeText(
                    this,
                    "Some permissions were denied – geofencing or notifications may not work properly.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Read all saved geofences from our local "database"
     * and register them with the Android geofencing system.
     *
     * This makes sure geofences are active again each time
     * the app starts (as long as we still have location permission).
     */
    private fun reRegisterGeofencesFromDb() {
        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(applicationContext).geofenceDao
            val savedGeofences = withContext(Dispatchers.IO) {
                dao.getAllGeofences()
            }

            if (savedGeofences.isEmpty()) {
                // Nothing to register
                return@launch
            }

            val geofencer = Geofencer(applicationContext)

            // Clear any existing geofences in the system,
            // then re-add all from our DB so DB is source of truth.
            geofencer.removeAllGeofences()

            savedGeofences.forEach { entity ->
                geofencer.addGeofence(
                    lat = entity.latitude,
                    lng = entity.longitude,
                    radiusMeters = entity.radiusMeters
                )
            }

            Toast.makeText(
                this@MainActivity,
                "Re-registered ${savedGeofences.size} geofence(s)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}