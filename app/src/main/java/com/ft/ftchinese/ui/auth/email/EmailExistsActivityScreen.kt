package com.ft.ftchinese.ui.auth.email

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.ft.ftchinese.ui.components.ProgressLayout

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
            onSuccess(it)
        }
    }

    ProgressLayout(
        loading = emailState.progress.value
    ) {
        EmailExistsScreen(
            loading = emailState.progress.value,
            onSubmit = {
                emailState.check(it)
            }
        )
    }
}
