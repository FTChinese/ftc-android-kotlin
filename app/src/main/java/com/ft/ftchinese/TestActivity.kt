package com.ft.ftchinese

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.fragment.app.commit
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.model.enums.*
import com.ft.ftchinese.model.ftcsubs.ConfirmationParams
import com.ft.ftchinese.model.ftcsubs.Order
import com.ft.ftchinese.model.legal.WebpageMeta
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.reader.Wechat
import com.ft.ftchinese.service.VerifySubsWorker
import com.ft.ftchinese.store.InvoiceStore
import com.ft.ftchinese.store.PayIntentStore
import com.ft.ftchinese.store.ServiceAcceptance
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.SubsActivity
import com.ft.ftchinese.ui.article.ArticleActivity
import com.ft.ftchinese.ui.base.ConnectionState
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.connectivityState
import com.ft.ftchinese.ui.base.toast
import com.ft.ftchinese.ui.checkout.LatestInvoiceActivity
import com.ft.ftchinese.ui.components.TextInput
import com.ft.ftchinese.ui.components.Timer
import com.ft.ftchinese.ui.components.rememberInputState
import com.ft.ftchinese.ui.components.rememberTimerState
import com.ft.ftchinese.ui.main.AcceptServiceDialogFragment
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OTheme
import com.ft.ftchinese.ui.webpage.WebpageActivity
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import okhttp3.internal.toLongOrDefault
import org.threeten.bp.LocalDate

private const val TAG = "TestActivity"

class TestActivity : ScopedAppActivity() {

    private lateinit var payIntentStore: PayIntentStore
    private lateinit var sessionManager: SessionManager
    private lateinit var workManager: WorkManager

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager.getInstance(this)
        payIntentStore = PayIntentStore.getInstance(this)
        workManager = WorkManager.getInstance(this)

        setContent {
            OTheme {

                val state = rememberModalBottomSheetState(
                    initialValue = ModalBottomSheetValue.Hidden,
                    skipHalfExpanded = true
                )
                val scope = rememberCoroutineScope()
                ModalBottomSheetLayout(
                    sheetState = state,
                    sheetContent = {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(text = "hello")
                        }
                    }
                ) {

                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(text = "Test")},
                                navigationIcon = {
                                    IconButton(onClick = {
                                        finish()
                                    }) {
                                        Icon(Icons.Filled.ArrowBack, "")
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->

                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(innerPadding)
                        ) {

                            NetworkStatus()

                            Button(
                                onClick = {
                                    scope.launch { state.show() }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Click to show sheet")
                            }

                            Spacer(modifier = Modifier.height(Dimens.dp16))

                            ServiceAcceptance()

                            TestCountDown()

                            TestTimer()

                            PaywallButton()

                            WxMiniButton()

                            PostPurchaseButton()

                            FreeUser()

                            WxOnlyFreeUser()

                            LinkedWithAutoRenew()

                            LinkedWithOneOff()

                            VIPUser()
                            StandardUser()
                            PremiumUser()

                            StripeStandardUser()

                            StripeStandardMonth()

                            StripeAutoRenewOff()

                            StripeAutoRenewOffWithAddOn()

                            StripePremium()

                            IAPStandard()

                            IAPPremium()
                            IAPAddOn()
                            IAPAutoRenewOff()
                            IAPExpiredWithAddOn()
                        }
                    }
                }
            }
        }

        intent.extras?.let {
            for (key in it.keySet()) {
                val value = intent.extras?.get(key)
                Log.i(TAG, "Key: $key Value: $value")
            }
        }
    }

    @Composable
    fun TestCountDown() {
        val (running, setRunning) = remember {
            mutableStateOf(false)
        }

        val inputState = rememberInputState(
            initialValue = "10"
        )
        TextInput(
            label = "Timer length in seconds",
            state = inputState,
            keyboardType = KeyboardType.Number
        )
        Button(
            onClick = {
                setRunning(true)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Timer(
                totalTime = inputState.field.value.toLongOrDefault(60),
                isRunning = running,
                initialText = "获取短信",
                onFinish = { setRunning(false) }
            )
        }
    }

    @Composable
    fun TestTimer() {
        val timerState = rememberTimerState(
            totalTime = 10, 
            initialText = "Start Timer"
        )
        
        Button(
            onClick = timerState::start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = timerState.text.value)
        }
    }

    @Composable
    fun NetworkStatus() {
        val connection by connectivityState()

        val isConnected = connection === ConnectionState.Available

        val onOff = if (isConnected) "on" else "off"
        Text(text = "Network $onOff")
    }

    @Composable
    fun PaywallButton() {
        Button(
            onClick = {
                SubsActivity.start(this@TestActivity)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Compose Paywall")
        }
    }

    @Composable
    fun WxMiniButton() {
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                WebpageActivity.start(this@TestActivity, WebpageMeta(
                    title = "Test",
                    url = "http://192.168.1.42:8080"
                ))
            },
        ) {
            Text(text = "Test Wechat Mini Program")
        }
    }

    @Composable
    fun PostPurchaseButton() {
        Button(
            onClick = {
                InvoiceStore.getInstance(this@TestActivity)
                    .savePurchaseAction(PurchaseAction.BUY)
                LatestInvoiceActivity.start(this@TestActivity)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Address - Buy")
        }
    }

    @Composable
    fun FreeUser() {
        Button(
            onClick = {
                sessionManager.logout()
                sessionManager.saveAccount(AccountBuilder()
                    .withTier(null)
                    .build())
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Free User")
        }
    }

    @Composable
    fun WxOnlyFreeUser() {
        Button(
            onClick = {
                sessionManager.logout()
                sessionManager.saveAccount(AccountBuilder()
                    .withAccountKind(LoginMethod.WECHAT)
                    .withTier(null)
                    .build())
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Wx-only Free User")
        }
    }

    @Composable
    fun LinkedWithAutoRenew() {
        Button(
            onClick = {
                sessionManager.logout()
                sessionManager.saveAccount(
                    AccountBuilder()
                        .withPayMethod(PayMethod.STRIPE)
                        .withAutoRenewal(true)
                        .build()
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Linked Account with Stripe")
        }
    }

    @Composable
    fun LinkedWithOneOff() {
        Button(
            onClick = {
                sessionManager.logout()
                sessionManager.saveAccount(
                    AccountBuilder()
                        .withPayMethod(PayMethod.ALIPAY)
                        .build()
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Linked Account with One Off")
        }
    }

    @Composable
    fun StandardUser() {
        Button(
            onClick = {
                sessionManager.logout()
                sessionManager.saveAccount(AccountBuilder()
                    .build())
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Standard User")
        }
    }

    @Composable
    fun PremiumUser() {
        Button(
            onClick = {
                sessionManager.logout()
                sessionManager.saveAccount(
                    AccountBuilder()
                        .withTier(Tier.PREMIUM)
                        .build()
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Premium User")
        }
    }

    @Composable
    fun VIPUser() {
        Button(
            onClick = {
                sessionManager.logout()
                sessionManager.saveAccount(
                    AccountBuilder()
                        .withVip(true)
                        .build()
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "VIP User")
        }
    }


    @Composable
    fun StripeStandardUser() {
        Button(
            onClick = {
                sessionManager.logout()
                sessionManager.saveAccount(
                    AccountBuilder()
                        .withTier(Tier.STANDARD)
                        .withPayMethod(PayMethod.STRIPE)
                        .withAutoRenewal(true)
                        .build())
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Stripe Standard Year")
        }
    }

    @Composable
    private fun StripeStandardMonth() {
        Button(
            onClick = {
                sessionManager.logout()
                sessionManager.saveAccount(
                    AccountBuilder()
                        .withPayMethod(PayMethod.STRIPE)
                        .withAutoRenewal(true)
                        .withCycle(Cycle.MONTH)
                        .build()
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Stripe Standard Month")
        }
    }

    @Composable
    fun StripePremium() {
        Button(
            onClick = {
                sessionManager.logout()
                sessionManager.saveAccount(
                    AccountBuilder()
                        .withPayMethod(PayMethod.STRIPE)
                        .withAutoRenewal(true)
                        .withTier(Tier.PREMIUM)
                        .build()
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Stripe Premium")
        }
    }

    @Composable
    fun StripeAutoRenewOff() {
        Button(
            onClick = {
                sessionManager.logout()
                sessionManager.saveAccount(
                    AccountBuilder()
                        .withPayMethod(PayMethod.STRIPE)
                        .withAutoRenewal(false)
                        .build()
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Stripe Auto Renew Off")
        }
    }

    @Composable
    fun StripeAutoRenewOffWithAddOn() {
        Button(
            onClick = {
                sessionManager.logout()
                sessionManager.saveAccount(
                    AccountBuilder()
                        .withPayMethod(PayMethod.STRIPE)
                        .withAutoRenewal(true)
                        .withStdAddOn(30)
                        .withPrmAddOn(366)
                        .build()
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Stripe Auto Renew Off with Add-on")
        }
    }

    @Composable
    fun IAPPremium() {
        Button(
            onClick = {
                sessionManager.logout()
                sessionManager.saveAccount(
                    AccountBuilder()
                        .withPayMethod(PayMethod.APPLE)
                        .withAutoRenewal(true)
                        .build()
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "IAP Premium")
        }
    }

    @Composable
    fun IAPStandard() {
        Button(
            onClick = {
                sessionManager.logout()
                sessionManager.saveAccount(
                    AccountBuilder()
                        .withPayMethod(PayMethod.APPLE)
                        .withAutoRenewal(false)
                        .build()
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "IAP Standard")
        }
    }

    @Composable
    fun IAPAutoRenewOff() {
        Button(
            onClick = {
                sessionManager.logout()
                sessionManager.saveAccount(
                    AccountBuilder()
                        .withPayMethod(PayMethod.APPLE)
                        .withAutoRenewal(false)
                        .build()
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "IAP Auto Renew Off")
        }
    }

    @Composable
    fun IAPAddOn() {
        Button(
            onClick = {
                sessionManager.logout()
                sessionManager.saveAccount(
                    AccountBuilder()
                        .withPayMethod(PayMethod.APPLE)
                        .withAutoRenewal(true)
                        .withStdAddOn(31)
                        .withPrmAddOn(366)
                        .build()
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "IAP AddOn")
        }
    }


    @Composable
    fun IAPExpiredWithAddOn() {
        Button(
            onClick = {
                sessionManager.logout()
                sessionManager.saveAccount(
                    AccountBuilder()
                        .withPayMethod(PayMethod.APPLE)
                        .withAutoRenewal(false)
                        .withExpired(true)
                        .withStdAddOn(31)
                        .withPrmAddOn(366)
                        .build()
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "IAP Expired with AddOn")
        }
    }


    @Composable
    fun FCMSubscribeTopic() {
        Button(onClick = {
            Log.i(TAG, "Subscribing to news topic")

            FirebaseMessaging
                .getInstance()
                .subscribeToTopic("news")
                .addOnCompleteListener {
                    if (!it.isSuccessful) {
                        toast("Subscription failed")
                    } else {
                        toast("Subscribed")
                    }
                }
        }) {
            Text(text = "FCM Subscribe a Topic")
        }
    }

    @Composable
    fun ShowPaymentResult() {
        Button(onClick = {
            createPaymentResult()
            LatestInvoiceActivity.start(this@TestActivity)
        }) {
            Text(text = "Show Payment Result")
        }
    }


    @Composable
    fun ShowUpgradeResult() {
        Button(onClick = {
            createUpgradeResult()
            LatestInvoiceActivity.start(this@TestActivity)
        }) {
            Text(text = "Show Upgrade Result")
        }
    }


    @Composable
    fun ServiceAcceptance() {
        Button(onClick = {
            supportFragmentManager.commit {
                add(android.R.id.content, AcceptServiceDialogFragment())
                addToBackStack(null)
            }
        }) {
            Text(text = "Service Acceptance")
        }
    }

    @Composable
    fun ClearServiceAcceptance() {
        Button(onClick = {
            ServiceAcceptance
                .getInstance(this@TestActivity)
                .clear()
        }) {
            Text(text = "Clear Service Acceptance")
        }
    }

    @Composable
    fun OneTimeWorkManager() {
        Button(onClick = {
            val request = OneTimeWorkRequestBuilder<VerifySubsWorker>()
                .build()
            workManager.enqueue(request)
        }) {
            Text(text = "One Time Work Manager")
        }
    }

    @Composable
    fun ShowLatestOrderActivity() {
        Button(onClick = {
            LatestInvoiceActivity
                .start(this@TestActivity)
        }) {
            Text(text = "Latest Order Activity")
        }
    }

    @Composable
    fun CreateNotificationChannel() {
        Button(onClick = {
            createNotiChannel()
        }) {
            Text(text = "Create Notification Channel")
        }
    }

    @Composable
    fun CreateLocalNotification() {
        Button(onClick = {
            createNotification()
        }) {
            Text(text = "Create Local Notification")
        }
    }

    private fun createPaymentResult() {
        InvoiceStore.
        getInstance(this@TestActivity).
        saveInvoices(
            ConfirmationParams(
                order = Order(
                    id = "order-id",
                    ftcId = "ftc-user-id",
                    unionId = null,
                    originalPrice = 298.0,
                    tier = Tier.STANDARD,
                    payableAmount = 298.0,
                    kind = OrderKind.Create,
                    payMethod = PayMethod.ALIPAY,
                    yearsCount = 1,
                    monthsCount = 0,
                    daysCount = 0,
                    confirmedAt = null,
                    startDate = null,
                    endDate =  null
                ),
                member = Membership()
            ).invoices
        )
    }

    private fun createUpgradeResult() {
        InvoiceStore
            .getInstance(this@TestActivity)
            .saveInvoices(
                ConfirmationParams(
                    order = Order(
                        id = "order-id",
                        ftcId = "ftc-user-id",
                        unionId = null,
                        originalPrice = 1998.0,
                        tier = Tier.PREMIUM,
                        payableAmount = 1998.0,
                        kind = OrderKind.Upgrade,
                        payMethod = PayMethod.ALIPAY,
                        yearsCount = 1,
                        monthsCount = 0,
                        daysCount = 0,
                        confirmedAt = null,
                        startDate = null,
                        endDate =  null
                    ),
                    member = Membership(
                        tier = Tier.STANDARD,
                        cycle = Cycle.YEAR,
                        expireDate = LocalDate.now().plusMonths(1),
                        payMethod = PayMethod.WXPAY,
                    )
                ).invoices
            )
    }

    private fun createNotiChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = getString(R.string.news_notification_channel_id)
            val channelName = getString(R.string.news_notification_channel_name)
            val channelDesc = getString(R.string.news_notification_channel_description)

            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = channelDesc
            }

            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            toast("$channelName created")
        }
    }

    private fun createNotification() {
        val intent = Intent(this, ArticleActivity::class.java).apply {
            putExtra(ArticleActivity.EXTRA_ARTICLE_TEASER, Teaser(
                    id = "001083331",
                    type = ArticleType.Story,
                    title = "波司登遭做空机构质疑 股价暴跌"
            ))
        }

        val pendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val builder = NotificationCompat.Builder(
                this,
                getString(R.string.news_notification_channel_id))
                .setSmallIcon(R.drawable.logo_round)
                .setContentTitle("波司登遭做空机构质疑 股价暴跌")
//                .setContentText("")
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText("周一，波司登的股价下跌了24.8%，随后宣布停牌。此前，做空机构Bonitas Research对波司登的收入和利润提出了质疑。"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(1, builder.build())
        }
    }


    companion object {

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, TestActivity::class.java))
        }
    }
}

@Composable
private fun ShowBottomSheet() {
    Button(
        onClick = { /*TODO*/ }
    ) {
        Text(text = "Show Bottom Sheet")
    }
}

private class AccountBuilder {

    private val ftcId = "0c726d53-2ec3-41e2-aa8c-5c4b0e23876a"
    private val unionId = "XiBumZN8QoFl4gO_gQS0ieqgjRU2"
    private val stripeCusId = "cus_Fo3kC8njfCdo"
    private val stripeSubId = "sub_65gBB0LBU3uD"
    private val iapTxId = "1000000799792245"
    private val licenceId = "lic_x0DaRxLHHuLG"
    private val mobile = "12345678901"

    private var accountKind: LoginMethod? = null
    private var tier: Tier? = Tier.STANDARD
    private var cycle = Cycle.YEAR
    private var payMethod = PayMethod.ALIPAY
    private var expired = false
    private var autoRenewal = false
    private var stdAddOn: Long = 0
    private var prmAddOn: Long = 0
    private var isVip = false


    fun withAccountKind(k: LoginMethod): AccountBuilder {
        accountKind = k
        return this
    }

    fun withTier(t: Tier?): AccountBuilder {
        tier = t
        return this
    }

    fun withCycle(c: Cycle): AccountBuilder {
        cycle = c
        return this
    }

    fun withPayMethod(p: PayMethod): AccountBuilder {
        payMethod = p
        return this
    }

    fun withExpired(yes: Boolean): AccountBuilder {
        expired = yes
        return this
    }

    fun withAutoRenewal(on: Boolean): AccountBuilder {
        autoRenewal = on
        return this
    }

    fun withStdAddOn(days: Long): AccountBuilder {
        stdAddOn = days
        return this
    }

    fun withPrmAddOn(days: Long): AccountBuilder {
        prmAddOn = days
        return this
    }

    fun withVip(yes: Boolean): AccountBuilder {
        isVip = yes
        return this
    }

    private fun buildMembership(): Membership {
        if (isVip) {
            return Membership(
                vip = true
            )
        }

        if (tier == null) {
            return Membership()
        }

        return Membership(
            tier = tier,
            cycle = cycle,
            expireDate = if (expired) {
                LocalDate.now().minusDays(1)
            } else {
                LocalDate.now().plusDays(1)
            },
            payMethod = payMethod,
            stripeSubsId = if (payMethod == PayMethod.STRIPE) {
                stripeSubId
            } else {
                null
            },
            autoRenew = autoRenewal,
            status = if (payMethod == PayMethod.STRIPE) {
                if (autoRenewal) {
                    StripeSubStatus.Active
                } else {
                    if (expired) {
                        StripeSubStatus.Canceled
                    } else {
                        StripeSubStatus.Active
                    }
                }
            } else {
                null
            },
            appleSubsId = if (payMethod == PayMethod.ALIPAY) {
                iapTxId
            } else {
                null
            },
            b2bLicenceId = if (payMethod == PayMethod.B2B) {
                licenceId
            } else {
                null
            },
            standardAddOn = stdAddOn,
            premiumAddOn = prmAddOn,
            vip = isVip,
        )
    }

    fun build(): Account {
        return Account(
            id = when (accountKind) {
                LoginMethod.WECHAT -> ""
                else -> ftcId
            },
            unionId = when (accountKind) {
                LoginMethod.EMAIL, LoginMethod.MOBILE -> ""
                else -> unionId
            },
            userName = "$payMethod $tier",
            email = "$tier@example.org",
            mobile = if (accountKind == LoginMethod.MOBILE) {
                mobile
            } else {
                null
            },
            isVerified = false,
            avatarUrl = null,
            campaignCode = null,
            loginMethod = accountKind,
            wechat = when (accountKind) {
                LoginMethod.EMAIL,
                LoginMethod.MOBILE -> Wechat()
                else -> {
                    Wechat(
                        nickname = "Wechat User",
                        avatarUrl = "https://randomuser.me/api/portraits/thumb/women/7.jpg"
                    )
                }
            },
            membership = buildMembership()
        )
    }
}
