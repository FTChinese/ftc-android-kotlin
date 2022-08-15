package com.ft.ftchinese.ui.auth.password

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.ft.ftchinese.model.reader.PwResetBearer
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.components.rememberTimerState

@Composable
fun ForgotActivityScreen(
    email: String?,
    scaffoldState: ScaffoldState,
    onVerified: (PwResetBearer) -> Unit,
) {
    val forgotState = rememberForgotState(
        scaffoldState = scaffoldState,
    )
    val (showAlert, setShowAlert) = remember {
        mutableStateOf(false)
    }
    val timerState = rememberTimerState()

    LaunchedEffect(key1 = forgotState.letterSent) {
        if (forgotState.letterSent) {
            timerState.start()
            setShowAlert(true)
        }
    }

    LaunchedEffect(key1 = forgotState.verified) {
        forgotState.verified?.let(onVerified)
    }

    if (showAlert) {
        AlertLetterSent {
            setShowAlert(false)
        }
    }

    ProgressLayout(
        loading = forgotState.progress.value,
        modifier = Modifier.fillMaxSize()
    ) {
        ForgotScreen(
            email = email,
            loading = forgotState.progress.value,
            timerState = timerState,
            onRequestCode = { email ->
                forgotState.requestCode(email)
            }
        ) {
            forgotState.verifyCode(it)
        }
    }
}
