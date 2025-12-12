package com.example.geofenceapplication.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.geofenceapplication.ui.theme.GeoFenceApplicationTheme

class GeofenceDetailActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val name = intent.getStringExtra("name") ?: "Unknown geofence"
        val latitude = intent.getDoubleExtra("lat", 0.0)
        val longitude = intent.getDoubleExtra("lng", 0.0)
        val radius = intent.getFloatExtra("radius", 0f)

        setContent {
            GeoFenceApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GeofenceDetailScreen(
                        name = name,
                        latitude = latitude,
                        longitude = longitude,
                        radiusMeters = radius,
                        onShare = {
                            shareGeofence(name, latitude, longitude, radius)
                        },
                        onBack = { finish() }
                    )
                }
            }
        }
    }

    private fun shareGeofence(
        name: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float
    ) {
        val shareText =
            "Geofence: $name\n" +
                    "Latitude: ${"%.5f".format(latitude)}\n" +
                    "Longitude: ${"%.5f".format(longitude)}\n" +
                    "Radius: ${radiusMeters.toInt()} m"

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        startActivity(Intent.createChooser(shareIntent, "Share geofence via"))
    }
}

@Composable
fun GeofenceDetailScreen(
    name: String,
    latitude: Double,
    longitude: Double,
    radiusMeters: Float,
    onShare: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Geofence Details",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Name: $name",
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Latitude: ${"%.5f".format(latitude)}",
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Longitude: ${"%.5f".format(longitude)}",
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Radius: ${radiusMeters.toInt()} m",
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )

        Button(onClick = onShare) {
            Text(text = "Share Geofence")
        }

        Button(onClick = onBack) {
            Text(text = "Back")
        }
    }
}