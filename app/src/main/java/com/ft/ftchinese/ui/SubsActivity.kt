package com.ft.ftchinese.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
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
import androidx.work.*
import com.alipay.sdk.app.PayTask
import com.ft.ftchinese.R
import com.ft.ftchinese.model.ftcsubs.AliPayIntent
import com.ft.ftchinese.model.ftcsubs.ConfirmationParams
import com.ft.ftchinese.model.ftcsubs.FtcPayIntent
import com.ft.ftchinese.model.paywall.CartItemFtc
import com.ft.ftchinese.service.VerifyOneTimePurchaseWorker
import com.ft.ftchinese.store.InvoiceStore
import com.ft.ftchinese.store.PayIntentStore
import com.ft.ftchinese.tracking.PaySuccessParams
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.base.ScopedComponentActivity
import com.ft.ftchinese.ui.base.toast
import com.ft.ftchinese.ui.checkout.LatestInvoiceActivity
import com.ft.ftchinese.ui.subs.SubsScreen
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.ftcpay.FtcPayActivityScreen
import com.ft.ftchinese.ui.subs.paywall.PaywallActivityScreen
import com.ft.ftchinese.ui.theme.OTheme
import com.ft.ftchinese.viewmodel.UserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SubsActivity : ScopedComponentActivity() {

    private lateinit var userViewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]

        val premiumFirst = intent.getBooleanExtra(EXTRA_PREMIUM_FIRST, false)

        setContent {
            SubsApp(
                userViewModel = userViewModel,
                premiumOnTop = premiumFirst,
                onAliPay = this::launchAliPay,
                onExit = {
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

            val resultStatus = payResult["resultStatus"]
            val msg = payResult["memo"] ?: getString(R.string.wxpay_failed)

            val tracker = StatsTracker.getInstance(this@SubsActivity)
            if (resultStatus != "9000") {
                toast(msg)
                tracker.payFailed(aliPayIntent.price.edition)

                return@launch
            }

            val pi = aliPayIntent.toPayIntent()
            confirmAliSubscription(aliPayIntent.toPayIntent())
            tracker.paySuccess(PaySuccessParams.ofFtc(pi))
        }
    }

    private fun confirmAliSubscription(pi: FtcPayIntent) {
        val account = userViewModel.accountLiveData.value ?: return
        val member = account.membership

        // Build confirmation result locally
        val confirmed = ConfirmationParams(
            order = pi.order,
            member = member
        ).buildResult()

        // Update membership.
        userViewModel.saveMembership(confirmed.membership)

        InvoiceStore.getInstance(this).save(confirmed)
        PayIntentStore.getInstance(this).save(pi.withConfirmed(confirmed.order))

        Log.i(TAG, "New membership: ${confirmed.membership}")

        toast(getString(R.string.subs_success))

        // Show the order details.
        LatestInvoiceActivity.start(this)
        setResult(Activity.RESULT_OK)

        // Schedule a worker to verify this order.
        verifyPayment()

        finish()
    }

    // Verify payment after alipay succeeded.
    private fun verifyPayment() {
        // Schedule VerifySubsWorker
        val verifyRequest: WorkRequest = OneTimeWorkRequestBuilder<VerifyOneTimePurchaseWorker>()
            .setConstraints(
                Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build())
            .build()

        WorkManager.getInstance(this).enqueue(verifyRequest)
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
        fun intent(context: Context) = Intent(context, SubsActivity::class.java)
    }
}

@Composable
fun SubsApp(
    userViewModel: UserViewModel,
    premiumOnTop: Boolean,
    onAliPay: (AliPayIntent) -> Unit,
    onExit: () -> Unit,
) {

    val scaffoldState = rememberScaffoldState()

    OTheme {

        val navController = rememberNavController()
        val backstackEntry = navController.currentBackStackEntryAsState()
        val currentScreen = SubsScreen.fromRoute(
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
                    route = SubsScreen.Paywall.name
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
                    FtcPayActivityScreen(
                        userViewModel = userViewModel,
                        scaffoldState = scaffoldState,
                        priceId = priceId,
                        onAliPay = onAliPay
                    )
                }

//                composable(
//                    route = "${SubsScreen.StripePay.name}/{priceId}?trialId={trialId}",
//                    arguments = listOf(
//                        navArgument("priceId") {
//                            type = NavType.StringType
//                        },
//                        navArgument("trialId") {
//                            type = NavType.StringType
//                        }
//                    )
//                ) { entry ->
//                    val priceId = entry.arguments?.getString("priceId")
//                    val trialId = entry.arguments?.getString("trialId")
//                    StripePayActivityScreen(
//                        pwViewModel = paywallViewModel,
//                        priceId = priceId,
//                        trialId = trialId,
//                        showSnackBar = { msg ->
//                            scope.launch {
//                                scaffoldState.snackbarHostState.showSnackbar(msg)
//                            }
//                        },
//                        onExit = {
//                            navController.popBackStack()
//                        }
//                    )
//                }
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

//private fun navigateToStripePay(
//    navController: NavHostController,
//    priceId: String,
//    trialId: String?,
//) {
//    navController.navigate("${SubsScreen.StripePay.name}/$priceId?trialId=${trialId}")
//}


