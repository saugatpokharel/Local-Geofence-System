package com.example.geofenceapplication.ui

import android.Manifest
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.FusedLocationProviderClient

@Composable
fun HomeScreen(
    fusedLocationClient: FusedLocationProviderClient
) {
    val context = LocalContext.current

    var locationText by remember { mutableStateOf("Location: unknown") }

    // Launcher to request fine location permission
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Use your existing helper from HomeFragment.kt
            getLastLocationCompose(fusedLocationClient) { newLocation ->
                locationText = newLocation
            }
        } else {
            Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
    ) {

        Text("Geofence App", style = MaterialTheme.typography.headlineMedium)

        // Show last known location
        Text(text = locationText)

        // Button to request location permission
        Button(onClick = {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }) {
            Text("Request Location Permission")
        }

        // Share app button
        Button(onClick = {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, "Try my GeoFence app!")
            context.startActivity(Intent.createChooser(intent, "Share using"))
        }) {
            Text("Share App")
        }

        // Automatically fetch last location if permission already granted
        LaunchedEffect(true) {
            checkAndFetchLocation(context, fusedLocationClient) { result ->
                locationText = result
            }
        }
    }
}
