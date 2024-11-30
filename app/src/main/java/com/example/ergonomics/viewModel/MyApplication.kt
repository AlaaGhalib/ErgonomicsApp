package com.example.ergonomics.viewModel

import android.content.Context

// Application class to provide application context for the ViewModel
class MyApplication : android.app.Application() {
    init {
        appContext = this
    }

    companion object {
        lateinit var appContext: Context
            private set
    }
}
