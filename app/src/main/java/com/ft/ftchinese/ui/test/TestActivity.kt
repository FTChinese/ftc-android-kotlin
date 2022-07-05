package com.ft.ftchinese.ui.test

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ft.ftchinese.R
import com.ft.ftchinese.model.content.Language
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.model.content.WebpageMeta
import com.ft.ftchinese.model.enums.*
import com.ft.ftchinese.model.ftcsubs.ConfirmationParams
import com.ft.ftchinese.model.ftcsubs.FtcPayIntent
import com.ft.ftchinese.model.ftcsubs.Order
import com.ft.ftchinese.model.paywall.defaultPaywall
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.reader.Wechat
import com.ft.ftchinese.service.VerifySubsWorker
import com.ft.ftchinese.store.InvoiceStore
import com.ft.ftchinese.store.PayIntentStore
import com.ft.ftchinese.store.ServiceAcceptance
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.article.ArticleActivity
import com.ft.ftchinese.ui.article.NavStore
import com.ft.ftchinese.ui.article.content.ArticleActivityScreen
import com.ft.ftchinese.ui.components.Heading3
import com.ft.ftchinese.ui.components.PrimaryBlockButton
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.components.rememberTimerState
import com.ft.ftchinese.ui.main.MainApp
import com.ft.ftchinese.ui.main.search.SearchActivityScreen
import com.ft.ftchinese.ui.main.terms.TermsActivityScreen
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OTheme
import com.ft.ftchinese.ui.util.ConnectionState
import com.ft.ftchinese.ui.util.connectivityState
import com.ft.ftchinese.ui.util.toast
import com.ft.ftchinese.ui.webpage.WebpageActivity
import com.ft.ftchinese.wxapi.WXPayEntryActivity
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate

private const val TAG = "TestActivity"

private enum class TestAppScreen(
    val title: String
) {
    Home("Test"),
    ModalBottomSheet("ModalBottomSheet"),
    MockUser("MockUser"),
    Search("Search"),
    Article("Article"),
    Agreement("Agreement"),
    Main("Main");

    companion object {
        @JvmStatic
        fun fromRoute(route: String?): TestAppScreen =
            when (route?.substringBefore("/")) {
                Home.name -> Home
                ModalBottomSheet.name -> ModalBottomSheet
                MockUser.name -> MockUser
                Search.name -> Search
                Article.name -> Article
                Agreement.name -> Agreement
                Main.name -> Main
                null -> Home
                else -> throw IllegalArgumentException("Route $route is not recognized")
            }
    }
}

class TestActivity : AppCompatActivity() {

    private lateinit var payIntentStore: PayIntentStore
    private lateinit var sessionManager: SessionManager
    private lateinit var workManager: WorkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager.getInstance(this)
        payIntentStore = PayIntentStore.getInstance(this)
        workManager = WorkManager.getInstance(this)

        setContent {
            TestApp(
                onFinish = { finish() }
            )
//            MainApp()
        }

        intent.extras?.let {
            for (key in it.keySet()) {
                val value = intent.extras?.get(key)
                Log.i(TAG, "Key: $key Value: $value")
            }
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
private fun TestApp(
    onFinish: () -> Unit
) {
    val scaffoldState = rememberScaffoldState()

    OTheme {
        val navController = rememberNavController()
        val backstackEntry = navController.currentBackStackEntryAsState()
        val currentScreen = TestAppScreen.fromRoute(
            backstackEntry.value?.destination?.route
        )

        Scaffold(
            topBar = {
                if (currentScreen != TestAppScreen.Main) {
                    Toolbar(
                        heading = currentScreen.title,
                        onBack = {
                            val ok = navController.popBackStack()
                            if (!ok) {
                                onFinish()
                            }
                        },
                    )
                }
            },
            scaffoldState = scaffoldState
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = TestAppScreen.Home.name,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(
                    route = TestAppScreen.Home.name
                ) {
                    TestActivityScreen {
                        navigateToScreen(
                            navController,
                            screen = it
                        )
                    }
                }

                composable(
                    route = TestAppScreen.ModalBottomSheet.name
                ) {
                    ModalBottomSheetScreen()
                }

                composable(
                    route = TestAppScreen.MockUser.name
                ) {
                    MockUserScreen()
                }

                composable(
                    route = TestAppScreen.Search.name
                ) {
                    SearchActivityScreen(
                        scaffoldState = scaffoldState,
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    route = TestAppScreen.Article.name
                ) {
                    val id = NavStore.saveTeaser(
                        Teaser(
                            id = "001096336",
                            type = ArticleType.Premium,
                            title = "我们为何工作得如此辛苦？",
                            tag = "工作生活平衡,职场,劳工权益,四天工作制,生产率,management,career",
                            isCreatedFromUrl = false,
                            hideAd = false,
                            langVariant = Language.CHINESE,
                        )
                    )
                    ArticleActivityScreen(
                        scaffoldState = scaffoldState,
                        id = id,
                        onScreenshot = { },
                        onAudio = {},
                        onArticle = {},
                        onChannel = {},
                        onBack = {}
                    )
                }

                composable(
                    route =  TestAppScreen.Agreement.name
                ) {
                    TermsActivityScreen(
                        onAgreed = { navController.popBackStack() },
                        onDeclined = { navController.popBackStack() }
                    )
                }

                composable(
                    route = TestAppScreen.Main.name
                ) {
                    MainApp(
                        userViewModel = viewModel()
                    )
                }
            }
        }
    }
}

private fun navigateToScreen(navController: NavController, screen: TestAppScreen) {
    navController.navigate(screen.name)
}

@Composable
private fun TestActivityScreen(
    onNavigate: (TestAppScreen) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(Dimens.dp16)
            .verticalScroll(rememberScrollState())
    ) {
        NetworkStatus()

        Divider()
        
        Heading3(text = "Entry Point")
        PrimaryBlockButton(
            onClick = { /*TODO*/ },
            text = "Show Agreement"
        )

        ClearServiceAcceptance()

        PrimaryBlockButton(
            onClick = {
                onNavigate(TestAppScreen.Main)
            },
            text = "Show Main Activity"
        )

        Divider()

        PrimaryBlockButton(
            onClick = { onNavigate(TestAppScreen.ModalBottomSheet) },
            text = "Show Bottom Sheet"
        )

        Divider()

        Heading3(text = "Content")
        PrimaryBlockButton(
            onClick = { onNavigate(TestAppScreen.Search) },
            text = "Test Search Bar"
        )

        PrimaryBlockButton(
            onClick = {
                onNavigate(TestAppScreen.Article)
            },
            text = "Example Article"
        )


        Divider()

        TestWxPay()

        TestTimer()
    }
}

@Composable
fun NetworkStatus() {
    val connection by connectivityState()

    val isConnected = connection === ConnectionState.Available

    val onOff = if (isConnected) "on" else "off"
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = "Network $onOff")
    }
}

@Composable
fun TestWxPay() {
    val context = LocalContext.current

    PrimaryBlockButton(
        onClick = {
            PayIntentStore.getInstance(context)
                .save(FtcPayIntent(
                    price = defaultPaywall.products[0].prices[0],
                    order = Order(
                        id = "test-order",
                        ftcId = "test-user",
                        unionId = null,
                        tier = Tier.STANDARD,
                        kind = OrderKind.AddOn,
                        payableAmount = 298.0,
                        payMethod = PayMethod.WXPAY,
                        yearsCount = 1,
                        monthsCount = 0,
                        daysCount = 0,
                    )
                ))

            WXPayEntryActivity.start(
                context,
                true
            )
        },
        text = "Launch Wx Pay Entry Activity"
    )
}

@Composable
fun TestTimer() {
    val timerState = rememberTimerState(
        totalTime = 10,
        initialText = "Start Timer"
    )

    PrimaryBlockButton(
        onClick = timerState::start,
        text = timerState.text.value
    )
}

@Composable
fun ClearServiceAcceptance() {
    val context = LocalContext.current
    PrimaryBlockButton(
        onClick = {
            ServiceAcceptance
                .getInstance(context)
                .clear()
        },
        text = "Clear Service Acceptance"
    )
}

@Composable
fun WxMiniButton() {
    val context = LocalContext.current
    PrimaryBlockButton(
        text = "Test Wechat Mini Program",
        onClick = {
            WebpageActivity.start(context, WebpageMeta(
                title = "Test",
                url = "http://192.168.1.42:8080"
            )
            )
        },
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ModalBottomSheetScreen() {
    val state = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = false
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
        PrimaryBlockButton(
            onClick = {
                scope.launch { state.show() }
            },
            text = "Click to show sheet"
        )
    }
}

@Composable
private fun MockUserScreen() {
    val context = LocalContext.current
    val session = SessionManager.getInstance(context)

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        PrimaryBlockButton(
            onClick = {
                session.logout()
                session.saveAccount(
                    AccountBuilder()
                        .withTier(null)
                        .build())
            },
            text = "Free User"
        )

        PrimaryBlockButton(
            onClick = {
                session.logout()
                session.saveAccount(
                    AccountBuilder()
                        .withAccountKind(LoginMethod.WECHAT)
                        .withTier(null)
                        .build())
            },
            text = "Wx-only Free User"
        )

        PrimaryBlockButton(
            onClick = {
                session.logout()
                session.saveAccount(
                    AccountBuilder()
                        .withPayMethod(PayMethod.STRIPE)
                        .withAutoRenewal(true)
                        .build()
                )
            },
            text = "Linked Account with Stripe"
        )

        PrimaryBlockButton(
            onClick = {
                session.logout()
                session.saveAccount(
                    AccountBuilder()
                        .withPayMethod(PayMethod.ALIPAY)
                        .build()
                )
            },
            text = "Linked Account with One Off"
        )

        PrimaryBlockButton(
            onClick = {
                session.logout()
                session.saveAccount(
                    AccountBuilder()
                        .build())
            },
            text = "Standard User"
        )

        PrimaryBlockButton(
            onClick = {
                session.logout()
                session.saveAccount(
                    AccountBuilder()
                        .withTier(Tier.PREMIUM)
                        .build()
                )
            },
            text = "Premium User"
        )


        PrimaryBlockButton(
            onClick = {
                session.logout()
                session.saveAccount(
                    AccountBuilder()
                        .withVip(true)
                        .build()
                )
            },
            text = "VIP User"
        )

        PrimaryBlockButton(
            onClick = {
                session.logout()
                session.saveAccount(
                    AccountBuilder()
                        .withTier(Tier.STANDARD)
                        .withPayMethod(PayMethod.STRIPE)
                        .withAutoRenewal(true)
                        .build())
            },
            text = "Stripe Standard Year"
        )

        PrimaryBlockButton(
            onClick = {
                session.logout()
                session.saveAccount(
                    AccountBuilder()
                        .withPayMethod(PayMethod.STRIPE)
                        .withAutoRenewal(true)
                        .withCycle(Cycle.MONTH)
                        .build()
                )
            },
            text = "Stripe Standard Month"
        )

        PrimaryBlockButton(
            onClick = {
                session.logout()
                session.saveAccount(
                    AccountBuilder()
                        .withPayMethod(PayMethod.STRIPE)
                        .withAutoRenewal(true)
                        .withTier(Tier.PREMIUM)
                        .build()
                )
            },
            text = "Stripe Premium"
        )

        PrimaryBlockButton(
            onClick = {
                session.logout()
                session.saveAccount(
                    AccountBuilder()
                        .withPayMethod(PayMethod.STRIPE)
                        .withAutoRenewal(false)
                        .build()
                )
            },
            text = "Stripe Auto Renew Off"
        )

        PrimaryBlockButton(
            onClick = {
                session.logout()
                session.saveAccount(
                    AccountBuilder()
                        .withPayMethod(PayMethod.STRIPE)
                        .withAutoRenewal(true)
                        .withStdAddOn(30)
                        .withPrmAddOn(366)
                        .build()
                )
            },
            text = "Stripe Auto Renew Off with Add-on"
        )

        PrimaryBlockButton(
            onClick = {
                session.logout()
                session.saveAccount(
                    AccountBuilder()
                        .withPayMethod(PayMethod.APPLE)
                        .withAutoRenewal(true)
                        .build()
                )
            },
            text = "IAP Premium"
        )

        PrimaryBlockButton(
            onClick = {
                session.logout()
                session.saveAccount(
                    AccountBuilder()
                        .withPayMethod(PayMethod.APPLE)
                        .withAutoRenewal(false)
                        .build()
                )
            },
            text = "IAP Standard"
        )

        PrimaryBlockButton(
            onClick = {
                session.logout()
                session.saveAccount(
                    AccountBuilder()
                        .withPayMethod(PayMethod.APPLE)
                        .withAutoRenewal(false)
                        .build()
                )
            },
            text = "IAP Auto Renew Off"
        )

        PrimaryBlockButton(
            onClick = {
                session.logout()
                session.saveAccount(
                    AccountBuilder()
                        .withPayMethod(PayMethod.APPLE)
                        .withAutoRenewal(true)
                        .withStdAddOn(31)
                        .withPrmAddOn(366)
                        .build()
                )
            },
            text = "IAP AddOn"
        )


        PrimaryBlockButton(
            onClick = {
                session.logout()
                session.saveAccount(
                    AccountBuilder()
                        .withPayMethod(PayMethod.APPLE)
                        .withAutoRenewal(false)
                        .withExpired(true)
                        .withStdAddOn(31)
                        .withPrmAddOn(366)
                        .build()
                )
            },
            text = "IAP Expired with AddOn"
        )

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
