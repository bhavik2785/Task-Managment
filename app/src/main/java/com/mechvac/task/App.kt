package com.mechvac.task

import android.app.Application

/** Starts Firebase early, so alerts that arrive while the app is closed can be shown. */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Fcm.init(this)
    }
}
