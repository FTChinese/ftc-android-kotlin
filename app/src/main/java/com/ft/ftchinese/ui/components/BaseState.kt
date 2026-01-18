package com.ft.ftchinese.ui.components

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.util.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

open class BaseState(
    val scaffoldState: ScaffoldState,
    val scope: CoroutineScope,
    val resources: Resources,
    val connection: State<ConnectionState>
) {
    val progress = mutableStateOf(false)

    val isConnected: Boolean
        get() = connection.value == ConnectionState.Available

    fun ensureConnected(): Boolean {
        if (connection.value != ConnectionState.Available) {
            showNotConnected()
            return false
        }

        return true
    }

    fun showSnackBar(@StringRes id: Int) {
        try {
            showSnackBar(resources.getString(id))
        } catch (e: Exception) {

        }
    }

    fun showSnackBar(message: String) {
        scope.launch {
            scaffoldState.snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
        }
    }

    fun showNotConnected() {
        showSnackBar(resources.getString(R.string.prompt_no_network))
    }

    fun showRefreshed() {
        showSnackBar(R.string.refresh_success)
    }

    fun showSaved() {
        showSnackBar(R.string.prompt_saved)
    }

}
