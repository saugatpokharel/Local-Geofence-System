package com.example.geofenceapplication.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.geofenceapplication.ui.theme.GeoFenceApplicationTheme

class SensorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        setContent {
            GeoFenceApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background) {
                    AccelerometerScreen(sensorManager, accelerometer)
                }
            }
        }
    }
}

@Composable
fun AccelerometerScreen(
    sensorManager: SensorManager,
    accelerometer: Sensor?
) {
    var x by remember { mutableStateOf(0f) }
    var y by remember { mutableStateOf(0f) }
    var z by remember { mutableStateOf(0f) }

    // Register sensor listener when this screen is visible
    DisposableEffect(Unit) {
        (if (accelerometer != null) {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    x = event.values[0]
                    y = event.values[1]
                    z = event.values[2]
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            sensorManager.registerListener(
                listener,
                accelerometer,
                SensorManager.SENSOR_DELAY_UI
            )

            onDispose {
                sensorManager.unregisterListener(listener)
            }
        } else {
            // no accelerometer on this device – nothing to register
        }) as DisposableEffectResult
    }

    // Simple dynamic UI using the sensor
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {

        Text(
            text = "Accelerometer (using Jetpack Compose)",
            style = MaterialTheme.typography.titleMedium
        )

        Text(text = "X: ${"%.2f".format(x)}", fontSize = 20.sp)
        Text(text = "Y: ${"%.2f".format(y)}", fontSize = 20.sp)
        Text(text = "Z: ${"%.2f".format(z)}", fontSize = 20.sp)

        // Little ball that moves based on tilt (dynamic + practical)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val offsetX = (x * 5).coerceIn(-100f, 100f)
            val offsetY = (y * 5).coerceIn(-100f, 100f)

            Box(
                modifier = Modifier
                    .offset(x = offsetX.dp, y = -offsetY.dp) // invert y so tilt feels natural
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }

        Text(
            text = "Tilt your phone – the blue circle moves based on sensor data.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}