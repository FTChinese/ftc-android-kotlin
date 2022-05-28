package com.ft.ftchinese.ui.webpage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.model.legal.WebpageMeta
import com.ft.ftchinese.ui.components.MenuOpenInBrowser
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.theme.OTheme
import com.ft.ftchinese.ui.web.JsInterface
import com.ft.ftchinese.ui.web.SimpleWebView
import com.google.accompanist.web.rememberWebViewState

class WebpageActivity : ScopedAppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.getParcelableExtra<WebpageMeta>(EXTRA_WEB_META)
            ?.let {
                setContent {
                    WebpageActivityScreen(
                        pageMeta = it,
                        onExit = {
                            finish()
                        }
                    )
                }
            }
    }


    companion object {
        private const val EXTRA_WEB_META = "extra_webpage_meta"
        fun start(context: Context, meta: WebpageMeta) {
            val intent = Intent(context, WebpageActivity::class.java).apply {
                putExtra(EXTRA_WEB_META, meta)

            }
            context.startActivity(intent)
        }
    }
}

@Composable
fun WebpageActivityScreen(
    pageMeta: WebpageMeta,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()
    val webViewState = rememberWebViewState(url = pageMeta.url)

    OTheme {
        Scaffold(
            topBar = {
                Toolbar(
                    heading = pageMeta.title,
                    onBack = onExit,
                    actions = {
                        if (pageMeta.showMenu) {
                            MenuOpenInBrowser {
                                CustomTabsIntent
                                    .Builder()
                                    .build()
                                    .launchUrl(
                                        context,
                                        Uri.parse(pageMeta.url)
                                    )
                            }
                        }
                    }
                )
            },
            scaffoldState = scaffoldState
        ) { innerPadding ->

            ProgressLayout(
                loading = webViewState.isLoading,
                modifier = Modifier.padding(innerPadding)
            ) {
                SimpleWebView(
                    state = webViewState,
                    jsInterface = JsInterface()
                )
            }
        }
    }
}

