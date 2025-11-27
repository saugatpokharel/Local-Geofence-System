package com.example.geofenceapplication.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
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

        // This text comes from the intent extra passed by GeofencesFragment
        val geofenceText = intent.getStringExtra("text") ?: "No geofence selected"

        setContent {
            GeoFenceApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GeofenceDetailScreen(
                        geofenceText = geofenceText,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun GeofenceDetailScreen(
    geofenceText: String,
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
            text = geofenceText,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Button(onClick = onBack) {
            Text(text = "Back")
        }
    }
}