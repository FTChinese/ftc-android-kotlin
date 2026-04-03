package com.ft.ftchinese

import android.app.Application
import android.util.Log
import com.ft.ftchinese.repository.ApiConfig
import com.ft.ftchinese.repository.Endpoint
import com.ft.ftchinese.repository.PushClient

private const val TAG = "App"
private const val API_LOG_PREFIX = "[FTCApi]"

class App : Application() {

    companion object {
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i(
            TAG,
            "$API_LOG_PREFIX authBaseUrl=${ApiConfig.ofAuth.baseUrl} authMode=${ApiConfig.ofAuth.mode} buildType=${BuildConfig.BUILD_TYPE} flavor=${BuildConfig.FLAVOR} debug=${BuildConfig.DEBUG} pushRegister=${Endpoint.pushRegister}"
        )
        PushClient.syncRegistration()
    }
}
