package com.example.geofenceapplication.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class HomeFragment : Fragment() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    override fun onCreateView(inflater: android.view.LayoutInflater,
                              container: android.view.ViewGroup?,
                              savedInstanceState: Bundle?
    ): android.view.View {

        return ComposeView(requireContext()).apply {
            setContent {
                HomeScreenCompose(
                    fusedLocationClient = fusedLocationClient,
                    openSensors = {
                        startActivity(Intent(requireContext(), SensorActivity::class.java))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenCompose(
    fusedLocationClient: FusedLocationProviderClient,
    openSensors: () -> Unit
) {
    val context = LocalContext.current

    var locationText by remember { mutableStateOf("Location: unknown") }

    // PERMISSION LAUNCHER
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(text = locationText)

        Button(onClick = {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }) {
            Text("Request Location Permission")
        }

        Button(onClick = {
            openLocationInMapsCompose(context, locationText)
        }) {
            Text("Open in Maps")
        }

        Button(onClick = {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, "Try my GeoFence app!")
            context.startActivity(Intent.createChooser(intent, "Share using"))
        }) {
            Text("Share App")
        }

        Button(onClick = openSensors) {
            Text("Open Sensor Activity")
        }

        // Automatically fetch last location
        LaunchedEffect(true) {
            checkAndFetchLocation(context, fusedLocationClient) { result ->
                locationText = result
            }
        }
    }
}

@SuppressLint("MissingPermission")
fun getLastLocationCompose(
    fusedLocationClient: FusedLocationProviderClient,
    onResult: (String) -> Unit
) {
    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        if (location != null)
            onResult("Location: ${location.latitude}, ${location.longitude}")
        else
            onResult("Location: unknown")
    }
}

fun checkAndFetchLocation(
    context: android.content.Context,
    fusedLocationClient: FusedLocationProviderClient,
    onResult: (String) -> Unit
) {
    val hasPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (hasPermission) {
        getLastLocationCompose(fusedLocationClient, onResult)
    }
}

fun openLocationInMapsCompose(context: android.content.Context, text: String) {
    val regex = Regex("(-?\\d+\\.\\d+), (-?\\d+\\.\\d+)")
    val match = regex.find(text)

    if (match != null) {
        val lat = match.groupValues[1]
        val lng = match.groupValues[2]
        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Current Location)")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } else {
        Toast.makeText(context, "Location unavailable", Toast.LENGTH_SHORT).show()
    }
}