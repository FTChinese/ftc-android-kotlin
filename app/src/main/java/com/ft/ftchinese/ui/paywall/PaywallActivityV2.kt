package com.ft.ftchinese.ui.paywall

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.ft.ftchinese.R
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedComponentActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.theme.OTheme

class PaywallActivityV2 : ScopedComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val account = SessionManager
            .getInstance(this).loadAccount() ?: return

        val cache = FileCache(this)

        val paywallViewModel = ViewModelProvider(
            this,
            PaywallViewModelFactory(cache)
        )[PaywallViewModel::class.java]

        connectionLiveData.observe(this) {
            paywallViewModel.isNetworkAvailable.value = it
        }
        paywallViewModel.isNetworkAvailable.value = isConnected

        setContent {
            OTheme {
                val scaffoldState = rememberScaffoldState()
                val navController = rememberNavController()

                Scaffold(
                    topBar = {
                        Toolbar(
                            barTitle = stringResource(id = R.string.title_subscription),
                            onBack = { finish() }
                        )
                    },
                    scaffoldState = scaffoldState
                ) {
                    paywallViewModel.msgId?.let {
                        LaunchedEffect(scaffoldState.snackbarHostState) {
                            scaffoldState.snackbarHostState.showSnackbar(getString(it))
                        }
                    }

                    paywallViewModel.errMsg?.let {
                        LaunchedEffect(scaffoldState.snackbarHostState) {
                            scaffoldState.snackbarHostState.showSnackbar(it)
                        }
                    }

                    PaywallScreen(
                        vm = paywallViewModel,
                        membership = account.membership,
                    )
                }
            }
        }

        paywallViewModel.loadPaywall(account.isTest)
        paywallViewModel.loadStripePrices()
    }

    companion object {
        @JvmStatic
        fun start(context: Context?) {
            context?.startActivity(Intent(context, PaywallActivityV2::class.java))
        }
    }
}
