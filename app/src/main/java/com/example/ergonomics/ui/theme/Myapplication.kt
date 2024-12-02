package com.example.ergonomics.ui.theme

import android.content.Context

class MyApplication : android.app.Application() {
    init {
        appContext = this
    }

    companion object {
        lateinit var appContext: Context
            private set
    }
}