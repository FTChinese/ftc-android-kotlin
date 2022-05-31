package com.ft.ftchinese.wxapi.wxpay

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.LiveData
import com.ft.ftchinese.ui.components.ProgressLayout

private const val TAG = "WxPay"

@Composable
fun WxPayActivityScreen(
    uiStatusLiveData: LiveData<WxPayStatus>,
    onClickDone: () -> Unit,
    onSuccess: () -> Unit,
) {
    val status = uiStatusLiveData.observeAsState(WxPayStatus.Loading)
    Log.i(TAG, "Status live data: ${status.value}")

    LaunchedEffect(key1 = status.value) {
        if (status.value is WxPayStatus.Success) {
            onSuccess()
        }
    }

    ProgressLayout(
        loading = status.value is WxPayStatus.Loading,
    ) {
        WxPayScreen(
            status = status.value,
            onDone = onClickDone
        )
    }
}
