package com.ft.ftchinese.ui.checkout

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.InvoiceStore
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.components.SimpleDialog
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.theme.OTheme
import com.ft.ftchinese.ui.web.SimpleWebView
import com.ft.ftchinese.ui.web.rememberJsInterface
import com.ft.ftchinese.viewmodel.UserViewModel
import com.google.accompanist.web.rememberWebViewStateWithHTMLData

class BuyerInfoActivity : ComponentActivity() {

    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OTheme {

                val scaffoldState = rememberScaffoldState()

                Scaffold(
                    topBar = {
                        Toolbar(
                            heading = "完善信息",
                            onBack = { finish() }
                        )
                    }
                ) {
                    BuyerInfoActivityScreen(
                        scaffoldState = scaffoldState
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
fun BuyerInfoActivityScreen(
    userViewModel: UserViewModel = viewModel(),
    scaffoldState: ScaffoldState,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val accountState = userViewModel.accountLiveData.observeAsState()
    val account = accountState.value
    if (account == null) {
        onExit()
        return
    }

    val invoiceStore = remember {
        InvoiceStore.getInstance(context)
    }
    val fileCache = remember {
        FileCache(context)
    }

    val baseUrl = remember(account) {
        Config.discoverServer(account)
    }

    val infoState = rememberBuyerInfoState(
        scaffoldState = scaffoldState
    )

    val wvState = rememberWebViewStateWithHTMLData(
        data = infoState.htmlLoaded,
        baseUrl = baseUrl,
    )

    LaunchedEffect(key1 = Unit) {
        val action = invoiceStore.loadPurchaseAction()
        if (action == null) {
            onExit()
            return@LaunchedEffect
        }

        infoState.loadPage(
            account = account,
            action = action,
            cache = fileCache
        )
    }

    if (infoState.exit) {
        onExit()
    }

    if (infoState.alert.isNotBlank()) {
        SimpleDialog(
            title = "",
            body = infoState.alert,
            onDismiss = infoState::closeAlert,
            onConfirm = infoState::closeAlert,
            dismissText = null,
        )
    }

    val jsInterface = rememberJsInterface(callback = infoState)

    ProgressLayout(
        loading = infoState.progress.value,
    ) {
        if (infoState.htmlLoaded.isNotBlank()) {
            SimpleWebView(
                state = rememberWebViewStateWithHTMLData(
                    data = infoState.htmlLoaded,
                    baseUrl = baseUrl,
                ),
                jsInterface = jsInterface
            )
        }
    }
}
