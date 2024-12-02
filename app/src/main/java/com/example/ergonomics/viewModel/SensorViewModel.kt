import android.content.ContentValues
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File
import java.io.FileWriter
import java.io.IOException
import kotlin.math.atan2
import kotlin.math.sqrt

import com.github.mikephil.charting.data.Entry
import java.io.FileOutputStream

class SensorViewModel(private val context: Context) : ViewModel(), SensorEventListener {
    private var alpha = 0.3f  // Filter factor between 0 and 1 (tune as necessary)
    private var previousEWMA = 0.0f

    private val _ewmaAngleLiveData = MutableLiveData<Float>()
    val ewmaAngleLiveData: LiveData<Float> get() = _ewmaAngleLiveData

    private val _complementaryAngleLiveData = MutableLiveData<Float>()
    val complementaryAngleLiveData: LiveData<Float> get() = _complementaryAngleLiveData

    private var previousGyroAngle = 0.0f
    private var scalingFactor = 10f

    private val ewmaAngleList = mutableListOf<Float>()
    private val complementaryAngleList = mutableListOf<Float>()

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private var isMeasuring = false

    fun startMeasurement() {
        if (!isMeasuring) {
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
            gyroscope?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
            isMeasuring = true
        }
    }

    fun stopMeasurement() {
        if (isMeasuring) {
            sensorManager.unregisterListener(this)
            isMeasuring = false
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val angle = calculateAngleUsingAccelerometer(event.values[0], event.values[1], event.values[2])
            // Apply EWMA filter to accelerometer angle
            val ewmaAngle = applyEWMAFilter(angle)
            _ewmaAngleLiveData.value = ewmaAngle * scalingFactor // Apply scaling factor to EWMA
        } else if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            val gyroAngle = event.values[1]  // Example assuming y-axis is used
            val complementaryAngle = applyComplementaryFilter(previousGyroAngle, gyroAngle)
            previousGyroAngle = gyroAngle
            _complementaryAngleLiveData.value = complementaryAngle * scalingFactor // Apply scaling factor to complementary filter
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }

    private fun calculateAngleUsingAccelerometer(x: Float, y: Float, z: Float): Float {
        // Calculate angle of elevation using basic trigonometry
        return Math.toDegrees(atan2(y.toDouble(), sqrt((x * x + z * z).toDouble()))).toFloat()
    }

    private fun applyEWMAFilter(currentAngle: Float): Float {
        val ewmaAngle = alpha * currentAngle + (1 - alpha) * previousEWMA
        previousEWMA = ewmaAngle
        return ewmaAngle
    }

    private fun applyComplementaryFilter(accelAngle: Float, gyroAngle: Float): Float {
        // Complimentary filter for sensor fusion
        return alpha * accelAngle + (1 - alpha) * gyroAngle
    }

    fun saveFileInDownloads(context: Context, fileName: String, content: String) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                outputStream.write(content.toByteArray())
            }
            println("File saved to Downloads: $uri")
        }
    }

    fun exportData(context: Context) {
        val contentToMap = ewmaAngleList.map { it.toString() }.joinToString { "\n" }
        saveFileInDownloads(context, "Test", contentToMap)
    }
}