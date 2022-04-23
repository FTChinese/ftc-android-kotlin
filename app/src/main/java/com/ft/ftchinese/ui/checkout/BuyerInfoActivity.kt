package com.ft.ftchinese.ui.checkout

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.AlertDialog
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.JS_INTERFACE_NAME
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.theme.OTheme
import com.ft.ftchinese.ui.webpage.ChromeClient
import com.ft.ftchinese.ui.webpage.WVClient
import com.google.accompanist.web.WebContent
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewStateWithHTMLData

class BuyerInfoActivity : ComponentActivity() {

    private lateinit var viewModel: BuyerInfoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val account = SessionManager
            .getInstance(this)
            .loadAccount()
            ?: return

        viewModel = ViewModelProvider(this)[BuyerInfoViewModel::class.java]

        setContent {
            OTheme {
                Scaffold(
                    topBar = {
                        Toolbar(
                            heading = "完善信息",
                            onBack = { finish() }
                        )
                    }
                ) {
                    BuyerInfoScreen(
                        baseUrl = Config.discoverServer(account),
                        infoViewModel = viewModel,
                        wvClient = WVClient(this)
                    ) {
                        finish()
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun start(context: Context) {
            context.startActivity(
                Intent(context, BuyerInfoActivity::class.java)
            )
        }
    }
}

@Composable
fun BuyerInfoScreen(
    baseUrl: String,
    infoViewModel: BuyerInfoViewModel,
    wvClient: WVClient,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val wvState = rememberWebViewStateWithHTMLData(
        data = "Loading...",
    )
    val exitState by infoViewModel.exitLiveData.observeAsState(false)
    val alertState by infoViewModel.alertLiveData.observeAsState("")

    val inProgress by infoViewModel.progressLiveData.observeAsState(true)
    val htmlRendered by infoViewModel.htmlRendered.observeAsState()

    if (exitState) {
        onExit()
        return
    }

    if (alertState.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { infoViewModel.clearAlert() },
            text = {
                Text(text = alertState)
            },
            confirmButton = {
                TextButton(
                    onClick = { infoViewModel.clearAlert() }
                ) {
                    Text(text = stringResource(id = R.string.action_ok))
                }
            }
        )
    }

    when (val h = htmlRendered) {
        is FetchResult.LocalizedError -> {
            wvState.content = WebContent.Data(context.getString(h.msgId), baseUrl = "")
        }
        is FetchResult.TextError -> {
            wvState.content = WebContent.Data(h.text, baseUrl = "")
        }
        is FetchResult.Success -> {
            wvState.content = WebContent.Data(h.data, baseUrl = baseUrl)
        }
        else -> { }
    }

    LaunchedEffect(key1 = Unit) {
        infoViewModel.loadPage()
    }

    ProgressLayout(
        loading = inProgress,
    ) {

        WebView(
            state = wvState,
            modifier = Modifier.fillMaxSize(),
            captureBackPresses = false,
            onCreated = {
                it.settings.javaScriptEnabled = true
                it.settings.loadsImagesAutomatically = true
                it.settings.domStorageEnabled = true
                it.settings.databaseEnabled = true
                it.addJavascriptInterface(infoViewModel, JS_INTERFACE_NAME)
                it.webViewClient = wvClient
                it.webChromeClient = ChromeClient()
            },
        )
    }
}
