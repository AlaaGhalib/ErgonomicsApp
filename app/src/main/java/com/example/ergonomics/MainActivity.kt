import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.example.ergonomics.model.SensorData
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

    private val sensorViewModel: SensorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI elements
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        exportButton = findViewById(R.id.exportButton)

        // Initialize Sensor Manager and Accelerometer
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Observe LiveData to get updates in UI if needed
        sensorViewModel.sensorDataList.observe(this, Observer { sensorData ->
            // Update UI or store data accordingly
        })

        // Start Measurement Button
        startButton.setOnClickListener {
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
                isMeasuring = true
                Toast.makeText(this, "Measurement started", Toast.LENGTH_SHORT).show()
            }
        }

        // Stop Measurement Button
        stopButton.setOnClickListener {
            if (isMeasuring) {
                sensorManager.unregisterListener(this)
                isMeasuring = false
                Toast.makeText(this, "Measurement stopped", Toast.LENGTH_SHORT).show()
            }
        }

        // Export Data Button
        exportButton.setOnClickListener {
            if (checkPermission()) {
                exportData()
            } else {
                requestPermission()
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (isMeasuring) {
            val timestamp = System.currentTimeMillis()
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val elevationAngle = calculateAngleUsingLinearAcceleration(x, y, z)

            val sensorData = SensorData(
                timestamp = timestamp,
                elevationAngle = elevationAngle,
                algorithmType = "Linear"
            )
            sensorViewModel.addSensorData(sensorData)
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
                sensorViewModel.sensorDataList.value?.forEach { data ->
                    writer.append("${data.timestamp},${data.elevationAngle},${data.algorithmType}\n")
                }
                writer.flush()
            }
            Toast.makeText(this, "Data exported to ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to export data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateAngleUsingLinearAcceleration(x: Float, y: Float, z: Float): Float {
        // Placeholder for real calculation logic using linear acceleration only
        return Math.toDegrees(Math.atan2(y.toDouble(), z.toDouble())).toFloat()
    }
}
