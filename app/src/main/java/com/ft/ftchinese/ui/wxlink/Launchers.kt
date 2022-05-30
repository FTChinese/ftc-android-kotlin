package com.ft.ftchinese.ui.wxlink

import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult

fun launchWxLinkEmailActivity(
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    context: Context,
) {
    launcher.launch(
        WxLinkEmailActivity.newIntent(context)
    )
}
