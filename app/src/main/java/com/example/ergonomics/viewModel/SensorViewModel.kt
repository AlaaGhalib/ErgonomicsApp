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
import com.example.ergonomics.model.SensorData
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

    private val sensorDataList = mutableListOf<SensorData>()

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
        val timestamp = System.currentTimeMillis()

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val angle = calculateAngleUsingAccelerometer(event.values[0], event.values[1], event.values[2])
            val ewmaAngle = applyEWMAFilter(angle)
            val sensorData = SensorData(timestamp, ewmaAngle, "Linear")
            sensorDataList.add(sensorData) // Spara datan i listan
            _ewmaAngleLiveData.value = ewmaAngle
        } else if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            val gyroAngle = event.values[1] // Y-axeln
            val complementaryAngle = applyComplementaryFilter(previousGyroAngle, gyroAngle)
            previousGyroAngle = gyroAngle
            val sensorData = SensorData(timestamp, complementaryAngle, "Linear+Gyroscope")
            sensorDataList.add(sensorData) // Spara datan i listan
            _complementaryAngleLiveData.value = complementaryAngle
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
        return alpha * accelAngle + (1 - alpha) * gyroAngle
    }

    fun exportData(context: Context) {
        val content = sensorDataList.map {
            "${it.timestamp}, ${it.elevationAngle}, ${it.algorithmType}"
        }
        saveFileInDownloads(context, content, "sensor_data_export")
    }

    private fun saveFileInDownloads(context: Context, content: List<String>, fileName: String) : File {
        val fileToStore = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "$fileName.csv")

        val write = FileWriter(fileToStore)
        write.write("Timestamp, EWMA Angle, Algorithm Type\n")
        content.forEach { write.write(it + "\n") }
        write.flush()
        write.close()

        return fileToStore
    }
}