package com.ft.ftchinese.ui.auth.email

import android.content.res.Resources
import android.util.Log
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.repository.AuthClient
import com.ft.ftchinese.ui.util.ConnectionState
import com.ft.ftchinese.ui.util.connectivityState
import com.ft.ftchinese.ui.components.BaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class EmailExists(
    val email: String,
    val exists: Boolean,
)

private const val TAG = "EmailExists"

class EmailExistsState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    resources: Resources,
    connState: State<ConnectionState>,
) : BaseState(scaffoldState, scope, resources, connState) {
    var found by mutableStateOf<EmailExists?>(null)

    fun check(email: String) {
        if (!ensureConnected()) {
            return
        }

        progress.value = true
        scope.launch {
            val result = AuthClient.asyncEmailExists(email)
            progress.value = false

            when (result) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                    Log.i(TAG, "Email exists: ${result.data}")
                    found = EmailExists(
                        email = email,
                        exists = result.data
                    )
                }
            }
        }
    }
}

@Composable
fun rememberEmailExistsState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    resources: Resources = LocalContext.current.resources,
    connState: State<ConnectionState> = connectivityState()
) = remember(scaffoldState, resources, connState) {
    EmailExistsState(
        scaffoldState = scaffoldState,
        scope = scope,
        resources = resources,
        connState = connState
    )
}
