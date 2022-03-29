package com.ft.ftchinese.ui.base

import android.content.Context
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

abstract class ScopedFragment: Fragment(),
        CoroutineScope by MainScope() {

    protected lateinit var connectionLiveData: ConnectionLiveData

    override fun onAttach(context: Context) {
        super.onAttach(context)

        connectionLiveData = ConnectionLiveData(context)
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }
}
