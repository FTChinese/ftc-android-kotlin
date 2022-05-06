package com.ft.ftchinese.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun ShowToast(
    toast: ToastMessage?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    if (toast != null) {
        LaunchedEffect(key1 = toast) {
            getToastText(
                toast = toast,
                context = context
            ).let {
                Toast.makeText(
                    context,
                    it,
                    Toast.LENGTH_SHORT
                ).show()
            }

            onDismiss()
        }
    }
}

@Composable
fun ShowSnackBar(
    toast: ToastMessage?,
    scaffoldState: ScaffoldState,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    if (toast != null) {
        LaunchedEffect(key1 = toast) {
            scaffoldState.snackbarHostState.showSnackbar(
                getToastText(
                    toast = toast,
                    context = context,
                )
            )
            onDismiss()
        }
    }
}

fun getToastText(
    toast: ToastMessage,
    context: Context,
): String {
    return when (toast) {
        is ToastMessage.Resource -> context.getString(toast.id)
        is ToastMessage.Text -> toast.text
    }
}
