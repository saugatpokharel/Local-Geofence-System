package com.example.geofenceapplication.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {

        return ComposeView(requireContext()).apply {
            setContent {
                HomeScreenCompose(
                    fusedLocationClient = fusedLocationClient
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenCompose(
    fusedLocationClient: FusedLocationProviderClient,
) {
    val context = LocalContext.current

    var locationText by remember { mutableStateOf("Location: unknown") }

    // Status values for the summary card
    var locationEnabled by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var savedGeofenceCount by remember { mutableStateOf(0) }

    // PERMISSION LAUNCHER for location
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                locationEnabled = true
                getLastLocationCompose(fusedLocationClient) { newLocation ->
                    locationText = newLocation
                }
            } else {
                locationEnabled = false
                Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    // PERMISSION LAUNCHER for notifications (Android 13+)
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            notificationsEnabled = granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            if (!granted) {
                Toast.makeText(context, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // --- Status summary card (top) ---
        StatusSummaryCard(
            locationEnabled = locationEnabled,
            notificationsEnabled = notificationsEnabled,
            savedGeofenceCount = savedGeofenceCount
        )

        // Show last known location
        Text(text = locationText)

        // Request GPS location permission
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

        // Enable notifications (needed so geofence notifications show on Android 13+)
        Button(onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }) {
            Text("Enable Notifications")
        }

        // Automatically fetch last location + status on first open
        LaunchedEffect(true) {
            // Location permission status
            locationEnabled = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            // Notification permission status
            notificationsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            // Load number of saved geofences from local storage
            savedGeofenceCount = withContext(Dispatchers.IO) {
                AppDatabase.getInstance(context.applicationContext).geofenceDao.getAllGeofences().size
            }

            checkAndFetchLocation(context, fusedLocationClient) { result ->
                locationText = result
            }
        }
    }
}

@Composable
private fun StatusSummaryCard(
    locationEnabled: Boolean,
    notificationsEnabled: Boolean,
    savedGeofenceCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()          // ⬅️ full width
            .padding(bottom = 20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp), // ⬅️ more padding = bigger feel
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Status",
                style = MaterialTheme.typography.headlineSmall, // ⬅️ bigger title
                modifier = Modifier.padding(bottom = 8.dp)
            )

            StatusRow(
                label = "📍 Location",
                value = if (locationEnabled) "Enabled" else "Disabled"
            )

            StatusRow(
                label = "🔔 Notifications",
                value = if (notificationsEnabled) "Enabled" else "Disabled"
            )

            StatusRow(
                label = "🟢 Active geofences",
                value = "$savedGeofenceCount saved"
            )
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge // ⬅️ bigger text
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
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