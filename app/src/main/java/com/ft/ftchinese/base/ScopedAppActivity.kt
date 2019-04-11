package com.ft.ftchinese.base

import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

@kotlinx.coroutines.ExperimentalCoroutinesApi
abstract class ScopedAppActivity: AppCompatActivity(),
        CoroutineScope by MainScope() {
    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }
}