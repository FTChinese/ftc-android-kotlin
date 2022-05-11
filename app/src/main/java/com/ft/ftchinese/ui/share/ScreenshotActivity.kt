package com.ft.ftchinese.ui.share

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.article.ArticleActivity
import com.ft.ftchinese.ui.components.CloseBar
import com.ft.ftchinese.ui.theme.OTheme
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory

class ScreenshotActivity : ComponentActivity() {

    private lateinit var wxApi: IWXAPI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val screenshot = intent
            .getParcelableExtra<ArticleScreenshot>(EXTRA_SCREENSHOT)

        wxApi = WXAPIFactory.createWXAPI(
            this,
            BuildConfig.WX_SUBS_APPID,
            false
        )

        setContent {
            OTheme {
                Screen(
                    screenshot = screenshot,
                    onShareTo = { app, screenshot ->
                        share(
                            appId = app.id,
                            screenshot = screenshot
                        )
                    },
                    onExit = {
                        finish()
                    }
                )
            }
        }
    }

    private fun share(
        appId: SocialAppId,
        screenshot: ArticleScreenshot,
    ) {
        Log.i(ArticleActivity.TAG, "Share screenshot to $appId")
        grantUriPermission(
            "com.tencent.mm",
            screenshot.imageUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        val req = contentResolver
            .openInputStream(screenshot.imageUri)
            ?.use {  stream ->
                ShareUtils.wxShareScreenshotReq(
                    appId = appId,
                    stream = stream,
                    screenshot = screenshot
                )
            } ?: return

        wxApi.sendReq(req)
    }

    companion object {
        private const val EXTRA_SCREENSHOT = "extra_screenshot"

        @JvmStatic
        fun start(context: Context, screenshot: ArticleScreenshot) {
            context.startActivity(
                Intent(context, ScreenshotActivity::class.java).apply {
                    putExtra(EXTRA_SCREENSHOT, screenshot)
                }
            )
        }
    }
}

@Composable
private fun Screen(
    screenshot: ArticleScreenshot?,
    onShareTo: (SocialApp, ArticleScreenshot) -> Unit,
    onExit: () -> Unit,
) {

    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        CloseBar(
            onClose = onExit
        )

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .weight(1.0f)
        ) {
            AsyncImage(
                model = ImageRequest
                    .Builder(context)
                    .data(screenshot?.imageUri)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .diskCachePolicy(CachePolicy.DISABLED)
                    .build(),
                contentDescription = "",
                modifier = Modifier
                    .fillMaxWidth()
            )
        }

        Divider()

        SocialShareList(
            apps = listOf(
                SocialApp(
                    name = "好友",
                    icon = R.drawable.wechat,
                    id = SocialAppId.WECHAT_FRIEND
                ),
                SocialApp(
                    name = "朋友圈",
                    icon = R.drawable.moments,
                    id = SocialAppId.WECHAT_MOMENTS
                ),
            ),
            onShareTo = { app ->
                screenshot?.let {
                    onShareTo(
                        app,
                        it,
                    )
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewScreenshot() {
    Screen(
        screenshot = null,
        onShareTo = { _, _, ->

        },
        onExit = {}
    )
}
