package com.example.ergonomics

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.components.XAxis

class MainActivity : AppCompatActivity() {

    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var chart: LineChart
    private lateinit var sensorViewModel: SensorViewModel
    private val angleEntries = mutableListOf<Entry>()
    private var entryIndex = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Use the ViewModelFactory to pass context
        val factory = SensorViewModelFactory(applicationContext)
        sensorViewModel = ViewModelProvider(this, factory).get(SensorViewModel::class.java)

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
        val lineDataSet = LineDataSet(angleEntries, "Elevation Angle").apply {
            color = Color.BLUE
            setCircleColor(Color.BLUE)
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
        }
        chart.data = LineData(lineDataSet)

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


        // Add views to layout
        layout.addView(titleTextView)
        layout.addView(chart)
        layout.addView(startButton)
        layout.addView(stopButton)

        // Set the layout as the content view
        setContentView(layout)

        // Observe real-time sensor data
        sensorViewModel.angleLiveData.observe(this, Observer { angle ->
            addEntryToChart(angle)
        })
    }

    private fun addEntryToChart(angle: Float) {
        angleEntries.add(Entry(entryIndex++, angle))
        val lineDataSet = LineDataSet(angleEntries, "Elevation Angle").apply {
            color = Color.BLUE
            setCircleColor(Color.BLUE)
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
        }
        chart.data = LineData(lineDataSet)
        chart.notifyDataSetChanged()
        chart.invalidate()
    }

    private fun checkPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
    }
}
