package com.ft.ftchinese.ui.article.screenshot

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.ui.util.toast
import com.ft.ftchinese.ui.components.rememberWxApi
import com.ft.ftchinese.ui.article.share.ShareApp
import com.ft.ftchinese.ui.article.share.SocialShareList
import com.ft.ftchinese.ui.theme.OColor
import com.ft.ftchinese.ui.util.ShareUtils
import com.tencent.mm.opensdk.openapi.IWXAPI

@Composable
fun ScreenshotActivityScreen(
    id: String?,
) {

    val context = LocalContext.current

    if (id.isNullOrBlank()) {
        context.toast("Missing type, id or imageUrl")
        return
    }

    val wxApi = rememberWxApi()

    val screenshotState = rememberScreenshotState()

    LaunchedEffect(key1 = Unit) {
        screenshotState.loadImage(id)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        Column(
            modifier = Modifier
                .weight(1f)
                .background(OColor.black)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
        ) {
            screenshotState.bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Preview",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth,
                )
            }
        }

        Divider()

        SocialShareList(
            apps = ShareApp.screenshot,
            onShareTo = { app ->
                screenshotState.meta?.let {
                    launchShare(
                        context = context,
                        wxApi = wxApi,
                        app = app,
                        screenshot = it
                    )
                }
            }
        )
    }
}

private fun launchShare(
    context: Context,
    wxApi: IWXAPI,
    app: ShareApp,
    screenshot: ScreenshotMeta,
) {

    context.grantUriPermission(
        "com.tencent.mm",
        screenshot.imageUri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION
    )

    val req = context.contentResolver
        .openInputStream(screenshot.imageUri)
        ?.use {  stream ->
            ShareUtils.wxShareScreenshotReq(
                appId = app,
                stream = stream,
                screenshot = screenshot
            )
        } ?: return

    wxApi.sendReq(req)
}
