package com.ft.ftchinese.ui.share

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.article.ArticleActivity
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.theme.OColor
import com.ft.ftchinese.ui.theme.OTheme
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory

private const val TAG = "ScreenshotActivity"

class ScreenshotActivity : ComponentActivity() {

    private lateinit var wxApi: IWXAPI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val screenshot = intent
            .getParcelableExtra<ScreenshotMeta>(EXTRA_SCREENSHOT)

        Log.i(TAG, "Display screenshot $screenshot")

        wxApi = WXAPIFactory.createWXAPI(
            this,
            BuildConfig.WX_SUBS_APPID,
            false
        )

        setContent {

            OTheme {

                Scaffold(
                    topBar = {
                         Toolbar(
                             heading = "Preview",
                             icon = Icons.Default.Close,
                             onBack = { finish() }
                         )
                    },
                    scaffoldState = rememberScaffoldState()
                ) { innerPadding ->
                    Screen(
                        screenshot = screenshot,
                        modifier = Modifier.padding(innerPadding),
                        onShareTo = { app, screenshot ->
                            share(
                                appId = app.id,
                                screenshot = screenshot
                            )
                        },
                    )
                }

            }
        }
    }

    private fun share(
        appId: SocialAppId,
        screenshot: ScreenshotMeta,
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
        fun start(context: Context, screenshot: ScreenshotMeta) {
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
    screenshot: ScreenshotMeta?,
    modifier: Modifier = Modifier,
    onShareTo: (SocialApp, ScreenshotMeta) -> Unit,
) {

    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .weight(1.0f)
        ) {
            if (screenshot == null) {
                Text(text = "No screenshot found!")
            }

            AsyncImage(
                model = ImageRequest
                    .Builder(context)
                    .data(screenshot?.imageUri)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .diskCachePolicy(CachePolicy.DISABLED)
                    .build(),
                contentDescription = "",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .background(OColor.black)
                    .fillMaxSize()
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
//        screenshot = ScreenshotMeta(
//            imageUri = Uri.parse("content://media/external_primary/images/media/1921"),
//            title = "Test",
//            description = "Test"
//        ),
        screenshot = null,
        onShareTo = { _, _, ->

        },
    )
}
