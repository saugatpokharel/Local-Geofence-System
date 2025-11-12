package com.example.geofenceapplication.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment
import com.example.geofenceapplication.R

class GeofencesFragment : Fragment(R.layout.fragment_geofences) {

    private val dummyGeofences = listOf(
        "Home - 100m",
        "Work - 200m",
        "Gym - 150m"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listView = view.findViewById<ListView>(R.id.listGeofences)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, dummyGeofences)
        listView.adapter = adapter

        // Explicit intent on click
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val intent = Intent(requireContext(), GeofenceDetailActivity::class.java)
            intent.putExtra("text", dummyGeofences[position])
            startActivity(intent)
        }

        // Implicit intent on long click (share)
        listView.setOnItemLongClickListener { _, _, position, _ ->
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Geofence: ${dummyGeofences[position]}")
            }
            startActivity(Intent.createChooser(share, "Share via"))
            true
        }
    }
}
