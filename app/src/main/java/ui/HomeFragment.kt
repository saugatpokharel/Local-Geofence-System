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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
                HomeScreenCompose(fusedLocationClient = fusedLocationClient)
            }
        }
    }
}

@Composable
fun HomeScreenCompose(
    fusedLocationClient: FusedLocationProviderClient
) {
    val context = LocalContext.current

    var locationText by remember { mutableStateOf("Location: unknown") }

    // Status values for the summary card
    var locationEnabled by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var savedGeofenceCount by remember { mutableStateOf(0) }

    // Help dialog state
    var showHelpDialog by remember { mutableStateOf(false) }

    // Permission launcher for location
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                locationEnabled = true
                getLastLocationCompose(fusedLocationClient) { newLocation ->
                    locationText = newLocation
                }
            } else {
                locationEnabled = false
                Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    // Permission launcher for notifications (Android 13+)
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notificationsEnabled = granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            if (!granted) {
                Toast.makeText(context, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    // Help dialog
    if (showHelpDialog) {
        HelpDialog(
            onDismiss = { showHelpDialog = false }
        )
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

        // Request location permission
        Button(onClick = {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }) {
            Text("Request Location Permission")
        }

        // Share app
        Button(onClick = {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Try my GeoFence app!")
            }
            context.startActivity(Intent.createChooser(intent, "Share using"))
        }) {
            Text("Share App")
        }

        // Enable notifications (Android 13+)
        Button(onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                Toast.makeText(context, "Notifications enabled by default", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Enable Notifications")
        }

        // ✅ NEW: Help button
        Button(onClick = { showHelpDialog = true }) {
            Text("Help")
        }

        // Load status when screen opens
        LaunchedEffect(Unit) {
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

            // Load number of saved geofences
            savedGeofenceCount = withContext(Dispatchers.IO) {
                AppDatabase.getInstance(context.applicationContext).geofenceDao.getAllGeofences().size
            }

            // Fetch location if allowed
            checkAndFetchLocation(context, fusedLocationClient) { result ->
                locationText = result
            }
        }
    }
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How to use the app") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("• Go to the Map tab to create a geofence.")
                Text("• Long-press on the map to set the geofence location.")
                Text("• Use the slider to adjust the radius.")
                Text("• Saved geofences appear in the Saved Geofences tab.")
                Text("• Long-press a geofence in the list to delete it.")
                Text("• Entering or exiting a geofence triggers an alert.")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

/* Bigger Status card */
@Composable
private fun StatusSummaryCard(
    locationEnabled: Boolean,
    notificationsEnabled: Boolean,
    savedGeofenceCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Status",
                style = MaterialTheme.typography.headlineSmall,
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
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@SuppressLint("MissingPermission")
fun getLastLocationCompose(
    fusedLocationClient: FusedLocationProviderClient,
    onResult: (String) -> Unit
) {
    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        if (location != null) {
            onResult("Location: ${location.latitude}, ${location.longitude}")
        } else {
            onResult("Location: unknown")
        }
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