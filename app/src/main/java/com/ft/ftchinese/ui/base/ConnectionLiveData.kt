package com.ft.ftchinese.ui.base

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import androidx.lifecycle.LiveData

class ConnectionLiveData(private val context: Context) : LiveData<Boolean>() {

    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            postValue(context?.isConnected)
        }
    }

    override fun onActive() {
        super.onActive()

        context.registerReceiver(
            networkReceiver,
            IntentFilter(ConnectivityManager.EXTRA_NO_CONNECTIVITY)
        )
    }

    override fun onInactive() {
        super.onInactive()
        try {
            context.unregisterReceiver(networkReceiver)
        } catch (e: Exception) {

        }
    }
}
