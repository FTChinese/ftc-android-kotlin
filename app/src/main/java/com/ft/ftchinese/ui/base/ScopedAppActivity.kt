package com.ft.ftchinese.ui.base

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

@kotlinx.coroutines.ExperimentalCoroutinesApi
abstract class ScopedAppActivity: AppCompatActivity(),
        CoroutineScope by MainScope() {

    protected lateinit var connectionLiveData: ConnectionLiveData

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        connectionLiveData = ConnectionLiveData(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }
}
