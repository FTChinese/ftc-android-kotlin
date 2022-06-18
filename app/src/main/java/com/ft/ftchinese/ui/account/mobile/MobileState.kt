package com.ft.ftchinese.ui.account.mobile

import android.content.res.Resources
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.BaseAccount
import com.ft.ftchinese.model.request.MobileFormValue
import com.ft.ftchinese.model.request.SMSCodeParams
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.ui.util.ConnectionState
import com.ft.ftchinese.ui.util.connectivityState
import com.ft.ftchinese.ui.components.BaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MobileState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    resources: Resources,
    connState: State<ConnectionState>,
) : BaseState(scaffoldState, scope, resources, connState) {

    var codeSent by mutableStateOf(false)
        private set

    var updated by mutableStateOf<BaseAccount?>(null)
        private set

    fun requestSMSCode(ftcId: String, params: SMSCodeParams) {
        if (!ensureConnected()) {
            return
        }

        codeSent = false
        progress.value = true
        scope.launch {
            val result = AccountRepo.asyncRequestSMSCode(
                ftcId = ftcId,
                params = params
            )
            progress.value = false
            when (result) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                    showSnackBar("验证码已发送")
                    codeSent = result.data
                }
            }
        }
    }

    fun changeMobile(ftcId: String, params: MobileFormValue) {
        if (!ensureConnected()) {
            return
        }

        progress.value = true
        scope.launch {
            val result = AccountRepo.asyncUpdateMobile(
                ftcId = ftcId,
                params = params,
            )

            progress.value = false
            when (result) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                    showSaved()
                    updated = result.data
                }
            }
        }
    }
}

@Composable
fun rememberMobileState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    resources: Resources = LocalContext.current.resources,
    connState: State<ConnectionState> = connectivityState()
) = remember(scaffoldState, resources, connState) {
    MobileState(
        scaffoldState = scaffoldState,
        scope = scope,
        resources = resources,
        connState = connState
    )
}
