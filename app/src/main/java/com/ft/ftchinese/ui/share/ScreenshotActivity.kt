package com.ft.ftchinese.ui.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
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
                    ScreenshotPreview(
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

// See https://ngengesenior.medium.com/pick-image-from-gallery-in-jetpack-compose-5fa0d0a8ddaf
// https://developer.android.com/reference/android/provider/MediaStore.Images.Media
// https://developer.android.com/reference/android/graphics/ImageDecoder#createSource(android.content.ContentResolver,%20android.net.Uri)
fun loadImageAsBitmap(
    context: Context,
    uri: Uri
): Bitmap {
    return if (Build.VERSION.SDK_INT < 28) {
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    } else {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source)
    }
}

@Composable
fun ScreenshotPreview(
    screenshot: ScreenshotMeta?,
    modifier: Modifier = Modifier,
    onShareTo: (SocialApp, ScreenshotMeta) -> Unit,
) {

    val context = LocalContext.current
    val bitmap = remember {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(key1 = Unit) {
        screenshot?.let {
            bitmap.value = loadImageAsBitmap(context, it.imageUri)
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {

        Column(
            modifier = Modifier
                .weight(1f)
                .background(OColor.black)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
        ) {
            bitmap.value?.let {
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
    ScreenshotPreview(
        screenshot = ScreenshotMeta(
            imageUri = Uri.parse("content://media/external_primary/images/media/1921"),
            title = "Test",
            description = "Test"
        ),
//        screenshot = null,
        onShareTo = { _, _, ->

        },
    )
}
