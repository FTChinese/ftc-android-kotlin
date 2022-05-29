package com.ft.ftchinese.ui.account.mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.model.request.MobileFormValue
import com.ft.ftchinese.ui.components.TimerState
import com.ft.ftchinese.ui.components.rememberTimerState
import com.ft.ftchinese.ui.form.MobileForm
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun MobileScreen(
    currentMobile: String,
    loading: Boolean,
    timerState: TimerState,
    onRequestCode: (String) -> Unit, // Pass user entered mobile to host.
    onSave: (MobileFormValue) -> Unit, // Pass mobile number and verification code back.
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.dp16)
    ) {
        MobileForm(
            defaultMobile = currentMobile,
            loading = loading,
            timerState = timerState,
            onRequestCode = onRequestCode,
            onSave = onSave
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMobileScreen() {
    MobileScreen(
        currentMobile = "123456789",
        loading = false,
        timerState = rememberTimerState(),
        onRequestCode = {},
        onSave = {}
    )
}
