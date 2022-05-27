package com.ft.ftchinese.ui.auth.password

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import com.ft.ftchinese.model.request.PasswordResetParams
import com.ft.ftchinese.ui.components.ProgressLayout

@Composable
fun ResetActivityScreen(
    email: String,
    token: String?,
    scaffoldState: ScaffoldState,
    onSuccess: () -> Unit
) {
    val resetState = rememberResetPasswordState(
        scaffoldState = scaffoldState
    )

    if (resetState.success) {
        AlertPasswordReset {
            onSuccess()
        }
    }

    if (token.isNullOrBlank()) {
        resetState.showSnackBar("Missing parameter token")
        return
    }

    ProgressLayout(
        loading = resetState.progress.value
    ) {
        ResetScreen(
            email = email,
            loading = resetState.progress.value,
            onSubmit = {
                resetState.startReset(
                    PasswordResetParams(
                        token = token,
                        password = it,
                    )
                )
            }
        )
    }
}
