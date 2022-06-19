package com.ft.ftchinese.ui.subs

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alipay.sdk.app.PayTask
import com.ft.ftchinese.model.ftcsubs.AliPayIntent
import com.ft.ftchinese.model.paywall.CartItemFtc
import com.ft.ftchinese.model.paywall.CartItemStripe
import com.ft.ftchinese.ui.components.PlainTextButton
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.subs.contact.BuyerInfoActivityScreen
import com.ft.ftchinese.ui.subs.ftcpay.AliPayResult
import com.ft.ftchinese.ui.subs.ftcpay.FtcPayActivityScreen
import com.ft.ftchinese.ui.subs.ftcpay.FtcPayViewModel
import com.ft.ftchinese.ui.subs.invoice.LatestInvoiceActivityScreen
import com.ft.ftchinese.ui.subs.paywall.PaywallActivityScreen
import com.ft.ftchinese.ui.subs.stripepay.StripeSubActivityScreen
import com.ft.ftchinese.ui.theme.OTheme
import com.ft.ftchinese.ui.util.IntentsUtil
import com.ft.ftchinese.viewmodel.UserViewModel
import kotlinx.coroutines.*

class SubsActivity : ComponentActivity(), CoroutineScope by MainScope() {

    private lateinit var userViewModel: UserViewModel
    private lateinit var ftcPayViewModel: FtcPayViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        ftcPayViewModel = ViewModelProvider(this)[FtcPayViewModel::class.java]

        val premiumFirst = intent.getBooleanExtra(EXTRA_PREMIUM_FIRST, false)

        setContent {
            SubsApp(
                userViewModel = userViewModel,
                payViewModel = ftcPayViewModel,
                premiumOnTop = premiumFirst,
                onAliPay = this::launchAliPay,
                onExit = {
                    finish()
                },
                onPaid = {
                    setResult(Activity.RESULT_OK, IntentsUtil.accountRefreshed)
                    finish()
                }
            )
        }
    }

    private fun launchAliPay(aliPayIntent: AliPayIntent) {

        val params = aliPayIntent.params.app ?: return

        launch {
            /**
             * Result is a map:
             * {resultStatus=6001, result=, memo=操作已经取消。}
             * {resultStatus=4000, result=, memo=系统繁忙，请稍后再试}
             * See https://docs.open.alipay.com/204/105301/ in section 同步通知参数说明
             * NOTE result field is JSON but you cannot use it as JSON.
             * You could only use it as a string
             */
            val payResult = withContext(Dispatchers.IO) {
                PayTask(this@SubsActivity).payV2(params, true)
            }

            Log.i(TAG, "Alipay result: $payResult")

            ftcPayViewModel.setAliPayResult(AliPayResult(
                intent = aliPayIntent,
                result = payResult
            ))
        }
    }

    override fun onResume() {
        super.onResume()
        userViewModel.reloadAccount()
    }

    companion object {

        private const val TAG = "SubsActivity"
        private const val EXTRA_PREMIUM_FIRST = "extra_premium_first"

        @JvmStatic
        fun start(context: Context?, premiumFirst: Boolean = false) {
            context?.startActivity(
                Intent(context, SubsActivity::class.java).apply {
                    putExtra(EXTRA_PREMIUM_FIRST, premiumFirst)
                }
            )
        }

        @JvmStatic
        fun launch(
            launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
            context: Context,
            premiumFirst: Boolean = false
        ) {
            launcher.launch(
                Intent(context, SubsActivity::class.java).apply {
                    putExtra(EXTRA_PREMIUM_FIRST, premiumFirst)
                }
            )
        }
    }
}

@Composable
fun SubsApp(
    userViewModel: UserViewModel,
    payViewModel: FtcPayViewModel,
    premiumOnTop: Boolean,
    onAliPay: (AliPayIntent) -> Unit,
    onExit: () -> Unit,
    onPaid: () -> Unit,
) {

    val scaffoldState = rememberScaffoldState()

    OTheme {

        val navController = rememberNavController()
        val backstackEntry = navController.currentBackStackEntryAsState()
        val currentScreen = SubsAppScreen.fromRoute(
            backstackEntry.value?.destination?.route
        )

        Scaffold(
            topBar = {
                Toolbar(
                    heading = stringResource(id = currentScreen.titleId),
                    onBack = {
                        val ok = navController.popBackStack()
                        if (!ok) {
                            onExit()
                        }
                    }
                ) {
                    if (currentScreen == SubsAppScreen.BuyerInfo) {
                        PlainTextButton(
                            onClick = onPaid,
                            text = "跳过"
                        )
                    }
                }
            },
            scaffoldState = scaffoldState
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = SubsAppScreen.Paywall.name,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(
                    route = SubsAppScreen.Paywall.name
                ) {
                    PaywallActivityScreen(
                        userViewModel = userViewModel,
                        scaffoldState = scaffoldState,
                        premiumOnTop = premiumOnTop,
                        onFtcPay =  { item: CartItemFtc ->
                            navigateToFtcPay(
                                navController = navController,
                                priceId = item.price.id,
                            )
                        },
                        onStripePay = { item: CartItemStripe ->
                            navigateToStripePay(
                                navController,
                                item
                            )
                        }
                    )
                }

                composable(
                    route = "${SubsAppScreen.FtcPay.name}/{priceId}",
                    arguments = listOf(
                        navArgument("priceId") {
                            type = NavType.StringType
                        }
                    )
                ) { entry ->
                    val priceId = entry.arguments?.getString("priceId")
                    FtcPayActivityScreen(
                        userViewModel = userViewModel,
                        payViewModel = payViewModel,
                        scaffoldState = scaffoldState,
                        priceId = priceId,
                        onAliPay = onAliPay,
                        onSuccess = {
                            navigateToInvoices(
                                navController
                            )
                        }
                    )
                }

                composable(
                    route = SubsAppScreen.Invoices.name
                ) {
                    LatestInvoiceActivityScreen(
                        userViewModel = userViewModel,
                        onNext = {
                            navigateToBuyerInfo(
                                navController
                            )
                        }
                    )
                }

                composable(
                    route = SubsAppScreen.BuyerInfo.name
                ) {
                    BuyerInfoActivityScreen(
                        scaffoldState = scaffoldState,
                        onExit = onExit
                    )
                }

                composable(
                    route = "${SubsAppScreen.StripePay.name}/{priceId}?trialId={trialId}",
                    arguments = listOf(
                        navArgument("priceId") {
                            type = NavType.StringType
                        },
                        navArgument("trialId") {
                            type = NavType.StringType
                        }
                    )
                ) { entry ->
                    val priceId = entry.arguments?.getString("priceId")
                    val trialId = entry.arguments?.getString("trialId")
                    StripeSubActivityScreen(
                        userViewModel = userViewModel,
                        scaffoldState = scaffoldState,
                        priceId = priceId,
                        trialId = trialId,
                        onSuccess = onPaid
                    ) {
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}


private fun navigateToFtcPay(
    navController: NavHostController,
    priceId: String,
) {
    navController.navigate("${SubsAppScreen.FtcPay.name}/$priceId")
}

private fun navigateToStripePay(
    navController: NavHostController,
    item: CartItemStripe,
) {
    navController.navigate("${SubsAppScreen.StripePay.name}/${item.recurring.id}?trialId=${item.trial?.id}")
}

private fun navigateToInvoices(
    navController: NavHostController
) {
    navController.navigate(SubsAppScreen.Invoices.name)
}

private fun navigateToBuyerInfo(
    navController: NavHostController
) {
    navController.navigate(SubsAppScreen.BuyerInfo.name)
}
