package com.ft.ftchinese.ui.components

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
fun Toast(
    scaffoldState: ScaffoldState,
    message: String,
) {
    LaunchedEffect(key1 = scaffoldState.snackbarHostState) {
        scaffoldState.snackbarHostState.showSnackbar(message = message)
    }
}
