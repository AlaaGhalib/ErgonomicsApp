package com.example.ergonomics.model

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Environment
import android.widget.Toast
import com.example.ergonomics.viewModel.MyApplication
import java.io.File
import java.io.FileWriter
import java.io.IOException
import kotlin.math.atan2
import kotlin.math.sqrt


// Repository to handle sensor interactions and data management
class SensorRepository : SensorEventListener {
    private val sensorManager: SensorManager = MyApplication.appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private var isMeasuring = false
    private val sensorData = mutableListOf<String>()
    private var alpha = 0.5f // Filter factor for both EWMA and complementary filter
    private var previousEWMA = 0.0f

    fun startMeasurement() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        isMeasuring = true
    }

    fun stopMeasurement() {
        if (isMeasuring) {
            sensorManager.unregisterListener(this)
            isMeasuring = false
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (isMeasuring) {
            val timestamp = System.currentTimeMillis()
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    val angle = calculateAngleUsingAccelerometer(event.values[0], event.values[1], event.values[2])
                    val filteredAngle = applyEWMAFilter(angle)
                    sensorData.add("$timestamp, Angle(Accelerometer): $filteredAngle")
                }
                Sensor.TYPE_GYROSCOPE -> {
                    val gyroRate = event.values[1] // Considering rotation around the y-axis
                    val fusedAngle = calculateAngleWithFusion(gyroRate)
                    sensorData.add("$timestamp, Angle(Fusion): $fusedAngle")
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }

    private fun calculateAngleUsingAccelerometer(x: Float, y: Float, z: Float): Float {
        // Calculate angle of elevation using basic trigonometry
        return Math.toDegrees(atan2(y.toDouble(), sqrt((x * x + z * z).toDouble()))).toFloat()
    }

    private fun applyEWMAFilter(currentInput: Float): Float {
        // Apply EWMA filter
        val currentOutput = alpha * currentInput + (1 - alpha) * previousEWMA
        previousEWMA = currentOutput
        return currentOutput
    }

    private fun calculateAngleWithFusion(gyroRate: Float): Float {
        // Complementary filter combining linear acceleration (previousEWMA) and gyroscope rate
        val deltaT = 0.02f // Assuming SensorManager.SENSOR_DELAY_UI is approximately 20ms
        val accelAngle = previousEWMA
        val gyroAngle = gyroRate * deltaT
        val fusedAngle = alpha * (accelAngle) + (1 - alpha) * gyroAngle
        return fusedAngle
    }

    fun exportData(context: Context) {
        val path = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val file = File(path, "sensor_data.csv")
        try {
            FileWriter(file).use { writer ->
                writer.append("Timestamp, Data\n")
                sensorData.forEach { data ->
                    writer.append(data).append("\n")
                }
                writer.flush()
            }
            Toast.makeText(context, "Data exported to ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to export data", Toast.LENGTH_SHORT).show()
        }
    }
}