package com.example.geofenceapplication.ui

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
import androidx.lifecycle.lifecycleScope
import com.example.geofenceapplication.ui.theme.GeoFenceApplicationTheme
import data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceDetailActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // These come from the intent extras passed by GeofencesFragment
        val geofenceId = intent.getLongExtra("id", -1L)
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
                        canDelete = geofenceId > 0L,
                        onDelete = {
                            if (geofenceId > 0L) {
                                deleteGeofence(geofenceId)
                            }
                            finish()
                        },
                        onBack = { finish() }
                    )
                }
            }
        }
    }

    private fun deleteGeofence(id: Long) {
        val dao = AppDatabase.getInstance(applicationContext).geofenceDao
        lifecycleScope.launch(Dispatchers.IO) {
            dao.deleteGeofence(id)
        }
    }
}

@Composable
fun GeofenceDetailScreen(
    name: String,
    latitude: Double,
    longitude: Double,
    radiusMeters: Float,
    canDelete: Boolean,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
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

        if (canDelete) {
            Button(onClick = onDelete) {
                Text(text = "Delete Geofence")
            }
        }

        Button(onClick = onBack) {
            Text(text = "Back")
        }
    }
}