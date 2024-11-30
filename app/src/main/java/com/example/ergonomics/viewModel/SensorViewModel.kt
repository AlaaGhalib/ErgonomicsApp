import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import androidx.lifecycle.ViewModel
import com.example.ergonomics.model.SensorRepository

// ViewModel to handle the logic and sensor data processing
class SensorViewModel : ViewModel(), SensorEventListener {
    private val sensorRepository: SensorRepository = SensorRepository()

    fun startMeasurement() {
        sensorRepository.startMeasurement()
    }

    fun stopMeasurement() {
        sensorRepository.stopMeasurement()
    }

    override fun onSensorChanged(event: SensorEvent) {
        sensorRepository.onSensorChanged(event)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }

    fun exportData(context: Context) {
        sensorRepository.exportData(context)
    }
}