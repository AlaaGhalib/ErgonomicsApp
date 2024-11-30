package com.example.ergonomics.model

data class SensorData(
    val timestamp: Long,
    val elevationAngle: Float,
    val algorithmType: String // "Linear" or "Linear+Gyroscope"
)
