package com.example.ergonomics

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ergonomics.model.SensorRepository
import kotlin.math.atan2
import kotlin.math.sqrt

class SensorViewModel(context: Context) : ViewModel(), SensorEventListener {

    private val sensorRepository: SensorRepository = SensorRepository(context)
    private val _angleLiveData = MutableLiveData<Float>()
    val angleLiveData: LiveData<Float> get() = _angleLiveData

    fun startMeasurement() {
        sensorRepository.startMeasurement(this)
    }

    fun stopMeasurement() {
        sensorRepository.stopMeasurement(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val angle = calculateAngleUsingAccelerometer(event.values[0], event.values[1], event.values[2])
            _angleLiveData.value = angle
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }

    private fun calculateAngleUsingAccelerometer(x: Float, y: Float, z: Float): Float {
        return Math.toDegrees(atan2(y.toDouble(), sqrt((x * x + z * z).toDouble()))).toFloat()
    }
}
