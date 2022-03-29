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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ft.ftchinese.model.paywall.CartItemFtcV2
import com.ft.ftchinese.model.paywall.CartItemStripeV2
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.ui.base.ScopedComponentActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.components.SubsScreen
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.ftcpay.FtcPayScreen
import com.ft.ftchinese.ui.theme.OTheme
import kotlinx.coroutines.launch

class SubsActivity : ScopedComponentActivity() {

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

        val premiumFirst = intent.getBooleanExtra(EXTRA_PREMIUM_FIRST, false)

        paywallViewModel.putPremiumOnTop(premiumFirst)

        setContent {
            SubscriptionApp(
                paywallViewModel = paywallViewModel,
                onExit = { finish() },
            )
        }
    }

    companion object {

        private const val EXTRA_PREMIUM_FIRST = "extra_premium_first"

        @JvmStatic
        fun start(context: Context?, premiumFirst: Boolean = false) {
            context?.startActivity(
                Intent(context, SubsActivity::class.java).apply {
                    putExtra(EXTRA_PREMIUM_FIRST, premiumFirst)
                }
            )
        }
    }
}

@Composable
fun SubscriptionApp(
    paywallViewModel: PaywallViewModel,
    onExit: () -> Unit,
) {

    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()

    OTheme {

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
                composable(
                    SubsScreen.Paywall.name
                ) {
                    PaywallScreen(
                        paywallViewModel = paywallViewModel,
                        onFtcPay = { item: CartItemFtcV2 ->
                            navigateToFtcPay(
                                navController = navController,
                                priceId = item.price.id,
                            )
                        },
                        onStripePay = { item: CartItemStripeV2 ->
                            navigateToStripePay(
                                navController = navController,
                                priceId = item.recurring.id,
                                trialId = item.trial?.id,
                            )
                        },
                        onError = { msg ->
                            scope.launch {
                                scaffoldState.snackbarHostState.showSnackbar(msg)
                            }
                        }
                    )
                }

                composable(
                    route = "${SubsScreen.FtcPay.name}/{priceId}",
                    arguments = listOf(
                        navArgument("priceId") {
                            type = NavType.StringType
                        }
                    )
                ) { entry ->
                    val priceId = entry.arguments?.getString("priceId")

                    FtcPayScreen(
                        priceId = priceId,
                        showSnackBar = { msg ->
                            scope.launch {
                                scaffoldState.snackbarHostState.showSnackbar(msg)
                            }
                        }
                    )
                }

                composable(
                    route = "${SubsScreen.StripePay.name}/{priceId}?trialId={trialId}",
                    arguments = listOf(
                        navArgument("priceId") {
                            type = NavType.StringType
                        },
                        navArgument("trialId") {
                            type = NavType.StringType
                            nullable = true
                        }
                    )
                ) { entry ->
                    val priceId = entry.arguments?.getString("priceId")
                    val trialId = entry.arguments?.getString("trialId")
                    Text(text = SubsScreen.StripePay.name)
                    Text(text = "Price id $priceId. Trial id $trialId")
                }
            }
        }
    }
}

private fun navigateToFtcPay(
    navController: NavHostController,
    priceId: String,
) {
    navController.navigate("${SubsScreen.FtcPay.name}/$priceId")
}

private fun navigateToStripePay(
    navController: NavHostController,
    priceId: String,
    trialId: String?
) {
    navController.navigate("${SubsScreen.StripePay}/$priceId?trialId=${trialId}")
}
