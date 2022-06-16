package com.ft.ftchinese.ui.account.mobile

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import com.ft.ftchinese.model.request.SMSCodeParams
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.components.rememberTimerState
import com.ft.ftchinese.viewmodel.UserViewModel

private const val TAG = "MobileScreen"

@Composable
fun MobileActivityScreen(
    userViewModel: UserViewModel,
    scaffoldState: ScaffoldState,
) {
    val accountState = userViewModel.accountLiveData.observeAsState()
    val account = accountState.value

    val uiState = rememberMobileState(
        scaffoldState = scaffoldState
    )

    val timerState = rememberTimerState()

    if (account == null) {
        return
    }

    uiState.updated?.let {
        Log.i(TAG, "Save updated account")
        userViewModel.saveAccount(account.withBaseAccount(it))
    }

    LaunchedEffect(key1 = uiState.codeSent) {
        if (uiState.codeSent) {
            Log.i(TAG, "Start timer")
            timerState.start()
        }
    }

    ProgressLayout(
        loading = uiState.progress.value,
        modifier = Modifier.fillMaxSize()
    ) {
        MobileScreen(
            currentMobile = account.mobile ?: "",
            loading = uiState.progress.value,
            timerState = timerState,
            onRequestCode = {
                uiState.requestSMSCode(
                    ftcId = account.id,
                    params = SMSCodeParams(
                        mobile = it
                    )
                )
            },
            onSave = {
                uiState.changeMobile(
                    ftcId = account.id,
                    params = it,
                )
            }
        )
    }
}
