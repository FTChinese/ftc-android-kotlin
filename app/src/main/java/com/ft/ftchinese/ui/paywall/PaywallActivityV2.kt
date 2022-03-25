package com.ft.ftchinese.ui.paywall

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ft.ftchinese.model.paywall.CartItemFtcV2
import com.ft.ftchinese.model.paywall.CartItemStripeV2
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedComponentActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.components.SubsScreen
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.theme.OTheme

class PaywallActivityV2 : ScopedComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            SubscriptionApp(
                paywallViewModel = paywallViewModel,
                onExit = { finish() }
            )
        }
    }

    companion object {
        @JvmStatic
        fun start(context: Context?) {
            context?.startActivity(Intent(context, PaywallActivityV2::class.java))
        }
    }
}

@Composable
fun SubscriptionApp(
    paywallViewModel: PaywallViewModel,
    onExit: () -> Unit,
) {
    val account = SessionManager
        .getInstance(LocalContext.current)
        .loadAccount()
        ?: return

    OTheme {
        val scaffoldState = rememberScaffoldState()
        val navController = rememberNavController()
        val backstackEntry = navController.currentBackStackEntryAsState()
        val currentScreen = SubsScreen.fromRoute(
            backstackEntry.value?.destination?.route
        )
        Scaffold(
            topBar = {
                Toolbar(
                    currentScreen = currentScreen,
                    onBack = {
                         val ok = navController.popBackStack()
                        if (!ok) {
                            onExit()
                        }
                    },
                )
            },
            scaffoldState = scaffoldState
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = SubsScreen.Paywall.name,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(SubsScreen.Paywall.name) {
                    PaywallScreen(
                        vm = paywallViewModel,
                        account = account,
                        scaffoldState = scaffoldState,
                        onFtcPay = { item: CartItemFtcV2 ->
                            navController.navigate(SubsScreen.FtcPay.name)
                        },
                        onStripePay = { item: CartItemStripeV2 ->
                            navController.navigate(SubsScreen.StripePay.name)
                        }
                    )
                }

                composable(SubsScreen.FtcPay.name) {
                    Text(text = SubsScreen.FtcPay.name)
                }

                composable(SubsScreen.StripePay.name) {
                    Text(text = SubsScreen.StripePay.name)
                }
            }
        }
    }
}
