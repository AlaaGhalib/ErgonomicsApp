package com.example.ergonomics

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileWriter
import java.io.IOException

class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var exportButton: Button
    private var isMeasuring = false
    private val sensorData: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create Layout Programmatically
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }

        // Initialize UI elements programmatically
        startButton = Button(this).apply {
            text = "Start Measurement"
            setOnClickListener {
                accelerometer?.let {
                    sensorManager.registerListener(this@MainActivity, it, SensorManager.SENSOR_DELAY_UI)
                    isMeasuring = true
                    Toast.makeText(this@MainActivity, "Measurement started", Toast.LENGTH_SHORT).show()
                }
            }
        }

        stopButton = Button(this).apply {
            text = "Stop Measurement"
            setOnClickListener {
                if (isMeasuring) {
                    sensorManager.unregisterListener(this@MainActivity)
                    isMeasuring = false
                    Toast.makeText(this@MainActivity, "Measurement stopped", Toast.LENGTH_SHORT).show()
                }
            }
        }

        exportButton = Button(this).apply {
            text = "Export Data"
            setOnClickListener {
                if (checkPermission()) {
                    exportData()
                } else {
                    requestPermission()
                }
            }
        }

        // Add buttons to layout
        layout.addView(startButton)
        layout.addView(stopButton)
        layout.addView(exportButton)

        // Set the layout as the content view
        setContentView(layout)

        // Initialize Sensor Manager and Accelerometer
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (isMeasuring) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val data = "X: $x, Y: $y, Z: $z"
            sensorData.add(data)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }

    private fun checkPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
    }

    private fun exportData() {
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(path, "sensor_data.csv")
        try {
            FileWriter(file).use { writer ->
                for (data in sensorData) {
                    writer.append(data).append("\n")
                }
                writer.flush()
            }
            Toast.makeText(this, "Data exported to ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to export data", Toast.LENGTH_SHORT).show()
        }
    }
}
