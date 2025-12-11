package com.example.geofenceapplication.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import data.AppDatabase
import data.GeofenceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GeofencesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return ComposeView(requireContext()).apply {

            setContent {

                // --- Compose state holding a list of geofences ---
                var geofences by remember { mutableStateOf<List<GeofenceEntity>>(emptyList()) }

                // --- Load geofences once when the screen is shown ---
                LaunchedEffect(Unit) {
                    val dao = AppDatabase.getInstance(requireContext()).geofenceDao
                    val list = withContext(Dispatchers.IO) {
                        dao.getAllGeofences()
                    }
                    geofences = list
                }

                // --- UI ---
                GeofencesScreen(
                    geofences = geofences,
                    onItemClick = { entity ->
                        val intent = Intent(requireContext(), GeofenceDetailActivity::class.java)
                        intent.putExtra("id", entity.id)
                        intent.putExtra("name", entity.name)
                        intent.putExtra("lat", entity.latitude)
                        intent.putExtra("lng", entity.longitude)
                        intent.putExtra("radius", entity.radiusMeters)
                        startActivity(intent)
                    },
                    onItemLongClick = { entity ->
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Geofence: ${entity.name}\n" +
                                        "Location: ${entity.latitude}, ${entity.longitude}\n" +
                                        "Radius: ${entity.radiusMeters}m"
                            )
                        }
                        startActivity(Intent.createChooser(share, "Share via"))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GeofencesScreen(
    geofences: List<GeofenceEntity>,
    onItemClick: (GeofenceEntity) -> Unit,
    onItemLongClick: (GeofenceEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Saved Geofences",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn {
            items(geofences) { item ->

                val displayText =
                    "${item.name} - ${item.radiusMeters.toInt()}m"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .combinedClickable(
                            onClick = { onItemClick(item) },
                            onLongClick = { onItemLongClick(item) }
                        )
                ) {
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}