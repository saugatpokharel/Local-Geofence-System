package com.example.geofenceapplication.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class GeofenceDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Simple layout for milestone 1
        val tv = TextView(this).apply {
            textSize = 20f
            text = intent.getStringExtra("text") ?: "Geofence Detail Screen"
            setPadding(50, 50, 50, 50)
        }

        setContentView(tv)
    }
}
