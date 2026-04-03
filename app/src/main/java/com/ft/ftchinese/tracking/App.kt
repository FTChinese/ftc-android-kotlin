package com.ft.ftchinese

import android.app.Application
import com.ft.ftchinese.repository.PushClient

class App : Application() {

    companion object {
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        PushClient.syncFcmRegistration()
    }
}
