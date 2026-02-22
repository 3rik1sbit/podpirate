package com.podpirate

import android.app.Application

class PodPirateApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PlaybackController.connect(this)
    }
}
