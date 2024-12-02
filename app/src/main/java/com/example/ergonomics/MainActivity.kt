package com.example.ergonomics

import SensorViewModel
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.components.XAxis

class MainActivity : AppCompatActivity() {
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var exportButton: Button
    private lateinit var chart: LineChart
    private val sensorViewModel: SensorViewModel by viewModels { SensorViewModelFactory(applicationContext) }
    private val ewmaAngleEntries = mutableListOf<Entry>()
    private val complementaryAngleEntries = mutableListOf<Entry>()
    private var entryIndex = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create Layout Programmatically
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.WHITE)
        }

        // Title TextView
        val titleTextView = TextView(this).apply {
            text = "Ergonomics Sensor App"
            textSize = 24f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 30
            }
        }

        // Initialize LineChart
        chart = LineChart(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                600
            ).apply {
                bottomMargin = 40
            }
            description.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            axisRight.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setBackgroundColor(Color.LTGRAY)
        }

        // Add initial empty dataset
        val ewmaLineDataSet = LineDataSet(ewmaAngleEntries, "EWMA Angle").apply {
            color = Color.RED
            setCircleColor(Color.RED)
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
        }
        val complementaryLineDataSet = LineDataSet(complementaryAngleEntries, "Complementary Angle").apply {
            color = Color.BLUE
            setCircleColor(Color.BLUE)
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
        }
        chart.data = LineData(ewmaLineDataSet, complementaryLineDataSet)

        // Initialize UI elements programmatically
        startButton = Button(this).apply {
            text = "Start Measurement"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
            setOnClickListener {
                sensorViewModel.startMeasurement()
                Toast.makeText(this@MainActivity, "Measurement started", Toast.LENGTH_SHORT).show()
            }
        }

        stopButton = Button(this).apply {
            text = "Stop Measurement"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
            setOnClickListener {
                sensorViewModel.stopMeasurement()
                Toast.makeText(this@MainActivity, "Measurement stopped", Toast.LENGTH_SHORT).show()
            }
        }

        exportButton = Button(this).apply {
            text = "Export Data"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                if (checkPermission()) {
                    sensorViewModel.exportData(this@MainActivity)
                } else {
                    requestPermission()
                }
            }
        }

        // Add views to layout
        layout.addView(titleTextView)
        layout.addView(chart)
        layout.addView(startButton)
        layout.addView(stopButton)
        layout.addView(exportButton)

        // Set the layout as the content view
        setContentView(layout)

        // Observe real-time sensor data
        sensorViewModel.ewmaAngleLiveData.observe(this, Observer { angle ->
            addEntryToChart(angle, isEwma = true)
        })
        sensorViewModel.complementaryAngleLiveData.observe(this, Observer { angle ->
            addEntryToChart(angle, isEwma = false)
        })
    }

    private fun addEntryToChart(angle: Float, isEwma: Boolean) {
        val normalizedAngle = normalizeAngle(angle)
        if (isEwma) {
            ewmaAngleEntries.add(Entry(entryIndex, normalizedAngle))
        } else {
            complementaryAngleEntries.add(Entry(entryIndex, normalizedAngle))
        }
        entryIndex++

        val ewmaLineDataSet = LineDataSet(ewmaAngleEntries, "EWMA Angle").apply {
            color = Color.RED
            setCircleColor(Color.RED)
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
        }
        val complementaryLineDataSet = LineDataSet(complementaryAngleEntries, "Complementary Angle").apply {
            color = Color.BLUE
            setCircleColor(Color.BLUE)
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
        }
        chart.data = LineData(ewmaLineDataSet, complementaryLineDataSet)
        chart.notifyDataSetChanged()
        chart.invalidate() // Refresh chart
    }

    private fun normalizeAngle(angle: Float): Float {
        // Normalize the angle to fit in a comparable range [0, 1]
        val minAngle = -90f // Assuming minimum angle is -90 degrees
        val maxAngle = 90f  // Assuming maximum angle is 90 degrees
        return (angle - minAngle) / (maxAngle - minAngle)
    }

    private fun checkPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            sensorViewModel.exportData(this)
    }
}
