package com.ft.ftchinese.wxapi

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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.model.ftcsubs.ConfirmationParams
import com.ft.ftchinese.model.ftcsubs.FtcPayIntent
import com.ft.ftchinese.store.InvoiceStore
import com.ft.ftchinese.store.PayIntentStore
import com.ft.ftchinese.tracking.PaySuccessParams
import com.ft.ftchinese.tracking.PaywallTracker
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.components.PlainTextButton
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.subs.SubsActivity
import com.ft.ftchinese.ui.subs.contact.BuyerInfoActivityScreen
import com.ft.ftchinese.ui.subs.invoice.LatestInvoiceActivityScreen
import com.ft.ftchinese.ui.theme.OTheme
import com.ft.ftchinese.viewmodel.UserViewModel
import com.ft.ftchinese.wxapi.wxpay.WxPayActivityScreen
import com.ft.ftchinese.wxapi.wxpay.WxPayStatus
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory

private const val TAG = "WxPayEntryActivity"

/**
 * https://pay.weixin.qq.com/wiki/doc/api/app/app.php?chapter=8_5
 * The the callback part of wechat pay.
 * Initially you should send request to server-side API to create an order. Pass the response data
 * to wechat sdk, which will call Wechat. After you paid inside wechat app, you will be redirected
 * to this page.
 */
class WXPayEntryActivity: ScopedAppActivity(), IWXAPIEventHandler {

    private var payIntentStore: PayIntentStore? = null
    private var tracker: StatsTracker? = null
    private var api: IWXAPI? = null

    private val statusLiveData: MutableLiveData<WxPayStatus> by lazy {
        MutableLiveData<WxPayStatus>()
    }
    private lateinit var userViewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        api = WXAPIFactory.createWXAPI(this, BuildConfig.WX_SUBS_APPID)

        payIntentStore = PayIntentStore.getInstance(this)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]

        tracker = StatsTracker.getInstance(this)

        api?.handleIntent(intent, this)

        setContent {
            WxPayApp(
                uiStatusLiveData = statusLiveData,
                onExit = this::onClickDone
            )
        }

        val isTest = intent.getBooleanExtra(EXTRA_TEST, false)
        if (isTest) {
            onResp(MockResp())
        }
    }

    /**
     * What does wechat send inside the intent:
     *
     * _mmessage_content: string
     * _mmessage_sdkVersion: int
     * _mmessage_appPackage: string
     * _mmessage_checksum: byte array
     * _wxapi_command_type: int
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        api?.handleIntent(intent, this)
    }

    // Reference https://pay.weixin.qq.com/wiki/doc/api/app/app.php?chapter=8_5
    /**
     * The BaseResp wraps a Bundle.
     * 4、支付结果回调
     * 参照微信SDK Sample，在net.sourceforge.simcpux.wxapi包路径中实现WXPayEntryActivity类(包名或类名不一致会造成无法回调)，
     * 在WXPayEntryActivity类中实现onResp函数，支付完成后，微信APP会返回到商户APP并回调onResp函数，
     * 开发者需要在该函数中接收通知，判断返回错误码，如果支付成功则去后台查询支付结果再展示用户实际支付结果。
     *  注意一定不能以客户端返回作为用户支付的结果，应以服务器端的接收的支付通知或查询API返回的结果为准.
     *  No this is crap. Use the result of client and schedule a background task to verify against server.
     */
    override fun onResp(resp: BaseResp?) {
        Log.i(TAG, "onPayFinish: type=${resp?.type}, errCode=${resp?.errCode}, errStr=${resp?.errStr}")

        val pi = payIntentStore?.load()

        when (resp?.type) {
            ConstantsAPI.COMMAND_PAY_BY_WX -> {
                when (resp.errCode) {
                    // 成功
                    // 展示成功页面
                    BaseResp.ErrCode.ERR_OK -> {
                        Log.i(TAG, "Start confirm subscription")
                        confirmSubscription(pi)
                    }
                    // 错误
                    // 可能的原因：签名错误、未注册APPID、项目设置APPID不正确、注册的APPID与设置的不匹配、其他异常等。
                    // Here the error message is possibly nullable.
                    BaseResp.ErrCode.ERR_COMM -> {
                        Log.i(TAG, "Call wx sdk failed: ${resp.errStr}")
                        statusLiveData.value = WxPayStatus.Error(resp.errStr ?: "Incorrect Wxpay SDK configuration! It could only be used in release version.")
                        // Tracking failure
                        pi?.let {
                            tracker?.payFailed(it.price.edition)
                        }
                    }
                    // 用户取消
                    // 无需处理。发生场景：用户不支付了，点击取消，返回APP。
                    BaseResp.ErrCode.ERR_USER_CANCEL -> {
                        Log.i(TAG, "Payment canceled: ${resp.errStr}")
                        statusLiveData.value = WxPayStatus.Canceled
                    }
                }
            }
            else -> {
                statusLiveData.value = WxPayStatus.Error("Unsupported wx sdk usage")
            }
        }
    }

    private fun confirmSubscription(pi: FtcPayIntent?) {

        // Load current membership
        val account = userViewModel.account ?: return
        val member = account.membership

        if (pi == null) {
            statusLiveData.value = WxPayStatus.Error("Missing payment intent!")
            return
        }

        // Confirm the order locally sand save it.
        val confirmed = ConfirmationParams(
            order = pi.order,
            member = member,
        ).buildResult()

        payIntentStore?.save(pi.withConfirmed(confirmed.order))
        InvoiceStore.getInstance(this).save(confirmed)

        userViewModel.saveMembership(confirmed.membership)

        statusLiveData.value = WxPayStatus.Success

        tracker?.paySuccess(PaySuccessParams.ofFtc(pi))
    }

    private fun onClickDone() {

        val pi = payIntentStore?.load()

        if (pi == null) {
            finish()
            return
        }

        if (pi.order.isConfirmed()) {
            PaywallTracker.from = null
            SubsActivity.start(this)
        }

        finish()
    }

    override fun onReq(req: BaseReq?) {

    }

    // Force back button to startForResult MembershipActivity so that use feels he is returning to previous MembershipActivity while actually the old instance already killed.
    // This hacking is used to refresh user data.
    // On iOS you do not need to handle it since there's no back button.
    override fun onBackPressed() {
        super.onBackPressed()
        onClickDone()
    }

    companion object {
        private const val EXTRA_TEST = "extra_test"
        @JvmStatic
        fun start(context: Context, isTest: Boolean = false) {
            context.startActivity(Intent(
                context,
                WXPayEntryActivity::class.java
            ).apply {
                putExtra(EXTRA_TEST, isTest)
            })
        }
    }
}

@Composable
fun WxPayApp(
    uiStatusLiveData: LiveData<WxPayStatus>,
    onExit: () -> Unit
) {
    val scaffold = rememberScaffoldState()

    OTheme {
        val navController = rememberNavController()
        val backstackEntry = navController.currentBackStackEntryAsState()

        val currentScreen = PayAppScreen.fromRoute(
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
                    if (currentScreen == PayAppScreen.BuyerInfo) {
                        PlainTextButton(
                            onClick = onExit,
                            text = "跳过"
                        )
                    }
                }
            },
            scaffoldState = scaffold,
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = PayAppScreen.PayResponse.name,
                modifier = Modifier.padding(innerPadding)
            ) {

                composable(
                    route = PayAppScreen.PayResponse.name
                ) {
                    WxPayActivityScreen(
                        uiStatusLiveData = uiStatusLiveData,
                        onFailure = onExit,
                        onSuccess = {
                            navigateToInvoice(
                                navController
                            )
                        }
                    )
                }

                composable(
                    route = PayAppScreen.Invoices.name
                ) {
                    LatestInvoiceActivityScreen(
                        onNext = {
                            navigateToBuyerInfo(
                                navController
                            )
                        }
                    )
                }

                composable(
                    route = PayAppScreen.BuyerInfo.name
                ) {
                    BuyerInfoActivityScreen(
                        scaffoldState = scaffold,
                        onExit = onExit
                    )
                }

            }
        }
    }
}

private fun navigateToInvoice(
    navController: NavController
) {
    navController.navigate(PayAppScreen.Invoices.name)
}

private fun navigateToBuyerInfo(
    navController: NavController
) {
    navController.navigate(PayAppScreen.BuyerInfo.name)
}
