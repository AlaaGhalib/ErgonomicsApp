import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ergonomics.model.SensorData

class SensorViewModel : ViewModel() {
    val sensorDataList = MutableLiveData<MutableList<SensorData>>()
    private val data = mutableListOf<SensorData>()

    fun addSensorData(sensorData: SensorData) {
        data.add(sensorData)
        sensorDataList.value = data
    }

    fun clearSensorData() {
        data.clear()
        sensorDataList.value = data
    }

    // Export logic can also be handled here if needed
}
