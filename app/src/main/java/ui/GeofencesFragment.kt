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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.compose.ui.platform.LocalContext
import com.example.geofenceapplication.geo.Geofencer
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

                val context = LocalContext.current
                val dao = remember { AppDatabase.getInstance(context).geofenceDao }
                val geofencer = remember { Geofencer(context.applicationContext) }
                val scope = rememberCoroutineScope()

                var geofences by remember { mutableStateOf<List<GeofenceEntity>>(emptyList()) }
                var geofenceToDelete by remember { mutableStateOf<GeofenceEntity?>(null) }

                // Initial load of geofences from DB
                LaunchedEffect(Unit) {
                    geofences = withContext(Dispatchers.IO) {
                        dao.getAllGeofences()
                    }
                }

                // Delete confirmation dialog
                geofenceToDelete?.let { entity ->
                    AlertDialog(
                        onDismissRequest = { geofenceToDelete = null },
                        title = {
                            Text(text = "Delete geofence")
                        },
                        text = {
                            Text(text = "Are you sure you want to delete \"${entity.name}\"?")
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val target = entity
                                    geofenceToDelete = null

                                    // 1) Update UI state immediately
                                    val updatedList = geofences.filter { it.id != target.id }
                                    geofences = updatedList

                                    // 2) Update DB + system geofences in background
                                    scope.launch(Dispatchers.IO) {
                                        dao.deleteGeofence(target.id)


                                        geofencer.removeGeofenceByRequestId("GEOFENCE_${target.id}")



                                    }
                                }
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { geofenceToDelete = null }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                GeofencesScreen(
                    geofences = geofences,
                    onItemClick = { entity ->
                        // Open detail activity with real data
                        val intent = Intent(context, GeofenceDetailActivity::class.java).apply {
                            putExtra("id", entity.id)
                            putExtra("name", entity.name)
                            putExtra("lat", entity.latitude)
                            putExtra("lng", entity.longitude)
                            putExtra("radius", entity.radiusMeters)
                        }
                        context.startActivity(intent)
                    },
                    onItemLongClick = { entity ->
                        // Trigger the delete confirmation dialog
                        geofenceToDelete = entity
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

                val displayText = "${item.name} - ${item.radiusMeters.toInt()}m"

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