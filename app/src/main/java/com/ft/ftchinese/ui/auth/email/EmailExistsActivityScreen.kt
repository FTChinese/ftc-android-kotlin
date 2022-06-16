package com.ft.ftchinese.ui.auth.email

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.ft.ftchinese.ui.components.ProgressLayout

private const val TAG = "EmailExists"

@Composable
fun EmailExistsActivityScreen(
    scaffoldState: ScaffoldState,
    onSuccess: (EmailExists) -> Unit
) {
    val emailState = rememberEmailExistsState(
        scaffoldState = scaffoldState
    )

    LaunchedEffect(key1 = emailState.found) {
        emailState.found?.let {
            Log.i(TAG, "Email exists result ${emailState.found}")
            onSuccess(it)
        }
    }

    ProgressLayout(
        loading = emailState.progress.value,
        modifier = Modifier.fillMaxSize()
    ) {
        EmailExistsScreen(
            loading = emailState.progress.value,
            onSubmit = {
                emailState.check(it)
            }
        )
    }
}
