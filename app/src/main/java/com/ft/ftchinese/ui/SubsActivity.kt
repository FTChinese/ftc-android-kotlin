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
import androidx.work.*
import com.alipay.sdk.app.PayTask
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.model.ftcsubs.AliPayIntent
import com.ft.ftchinese.model.ftcsubs.ConfirmationParams
import com.ft.ftchinese.model.ftcsubs.FtcPayIntent
import com.ft.ftchinese.model.ftcsubs.WxPayIntent
import com.ft.ftchinese.model.paywall.CartItemFtc
import com.ft.ftchinese.service.VerifyOneTimePurchaseWorker
import com.ft.ftchinese.ui.base.ScopedComponentActivity
import com.ft.ftchinese.ui.checkout.LatestInvoiceActivity
import com.ft.ftchinese.ui.components.SubsScreen
import com.ft.ftchinese.ui.components.ToastMessage
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.ftcpay.FtcPayActivityScreen
import com.ft.ftchinese.ui.ftcpay.FtcPayViewModel
import com.ft.ftchinese.ui.ftcpay.OrderResult
import com.ft.ftchinese.ui.paywall.PaywallActivityScreen
import com.ft.ftchinese.ui.paywall.PaywallViewModel
import com.ft.ftchinese.ui.theme.OTheme
import com.ft.ftchinese.viewmodel.UserViewModel
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.toast

class SubsActivity : ScopedComponentActivity() {

    private lateinit var wxApi: IWXAPI
    private lateinit var ftcPayViewModel: FtcPayViewModel
    private lateinit var paywallViewModel: PaywallViewModel
    private lateinit var userViewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        paywallViewModel = ViewModelProvider(this)[PaywallViewModel::class.java]
        ftcPayViewModel = ViewModelProvider(this)[FtcPayViewModel::class.java]
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]

        connectionLiveData.observe(this) {
            paywallViewModel.isNetworkAvailable.value = it
            ftcPayViewModel.isNetworkAvailable.value = it
        }

        val premiumFirst = intent.getBooleanExtra(EXTRA_PREMIUM_FIRST, false)

        paywallViewModel.putPremiumOnTop(premiumFirst)

        wxApi = WXAPIFactory.createWXAPI(this, BuildConfig.WX_SUBS_APPID)
        wxApi.registerApp(BuildConfig.WX_SUBS_APPID)

        ftcPayViewModel.orderLiveData.observe(this) { orderResult ->
            when (orderResult) {
                is OrderResult.WxPay -> {
                    Log.i(TAG, "Wx order ${orderResult.order}")
                    launchWxPay(orderResult.order)
                }
                is OrderResult.AliPay -> {
                    Log.i(TAG, "Ali order ${orderResult.order}")
                    launchAliPay(orderResult.order)
                }
            }
        }

        setContent {
            SubsApp(
                paywallViewModel = paywallViewModel,
                ftcPayViewModel = ftcPayViewModel,
                onExit = { finish() },
                wxApi = wxApi,
            )
        }

        paywallViewModel.toastLiveData.observe(this) {
            when (it) {
                is ToastMessage.Resource -> toast(it.id)
                is ToastMessage.Text -> toast(it.text)
            }
        }
    }

    private fun launchWxPay(wxPayIntent: WxPayIntent) {
        val params = wxPayIntent.params.app ?: return

        val result = wxApi.sendReq(params.buildReq())

        if (result) {
            setResult(Activity.RESULT_OK)
            finish()
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

            if (resultStatus != "9000") {
                toast(msg)
                ftcPayViewModel.trackFailedPay(aliPayIntent.toPayIntent())

                return@launch
            }

            confirmAliSubscription(aliPayIntent.toPayIntent())
        }
    }

    private fun confirmAliSubscription(pi: FtcPayIntent) {
        val account = userViewModel.account ?: return
        val member = account.membership

        // Build confirmation result locally
        val confirmed = ConfirmationParams(
            order = pi.order,
            member = member
        ).buildResult()

        // Update membership.
        userViewModel.saveMembership(confirmed.membership)

        ftcPayViewModel.saveConfirmation(confirmed, pi)
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
    paywallViewModel: PaywallViewModel,
    ftcPayViewModel: FtcPayViewModel,
    wxApi: IWXAPI,
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
                    route = SubsScreen.Paywall.name
                ) {
                    PaywallActivityScreen(
                        paywallViewModel = paywallViewModel
                    ) { item: CartItemFtc ->
                        navigateToFtcPay(
                            navController = navController,
                            priceId = item.price.id,
                        )
                    }
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
                        payViewModel = ftcPayViewModel,
                        pwViewModel = paywallViewModel,
                        wxApi = wxApi,
                        priceId = priceId,
                        showSnackBar = { msg ->
                            scope.launch {
                                scaffoldState.snackbarHostState.showSnackbar(msg)
                            }
                        }
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


