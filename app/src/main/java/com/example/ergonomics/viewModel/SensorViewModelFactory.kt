package com.example.ergonomics

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SensorViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SensorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SensorViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
