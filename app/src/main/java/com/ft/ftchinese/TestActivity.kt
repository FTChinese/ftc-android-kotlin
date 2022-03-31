package com.ft.ftchinese

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.lifecycle.ViewModelProvider
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.ft.ftchinese.model.content.ArticleType
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.model.enums.*
import com.ft.ftchinese.model.ftcsubs.ConfirmationParams
import com.ft.ftchinese.model.ftcsubs.Order
import com.ft.ftchinese.model.legal.WebpageMeta
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.LoginMethod
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.reader.Wechat
import com.ft.ftchinese.service.VerifySubsWorker
import com.ft.ftchinese.store.InvoiceStore
import com.ft.ftchinese.store.PayIntentStore
import com.ft.ftchinese.store.ServiceAcceptance
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.article.ArticleActivity
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.checkout.BuyerInfoActivity
import com.ft.ftchinese.ui.checkout.LatestInvoiceActivity
import com.ft.ftchinese.ui.login.AuthActivity
import com.ft.ftchinese.ui.login.SignInFragment
import com.ft.ftchinese.ui.login.SignUpFragment
import com.ft.ftchinese.ui.mobile.MobileViewModel
import com.ft.ftchinese.ui.subsactivity.SubsActivity
import com.ft.ftchinese.ui.theme.OTheme
import com.ft.ftchinese.ui.webpage.WebpageActivity
import com.google.firebase.messaging.FirebaseMessaging
import org.jetbrains.anko.alert
import org.jetbrains.anko.appcompat.v7.Appcompat
import org.jetbrains.anko.toast
import org.threeten.bp.LocalDate

private const val TAG = "TestActivity"

class TestActivity : ScopedAppActivity() {

    private lateinit var payIntentStore: PayIntentStore
    private lateinit var sessionManager: SessionManager
    private lateinit var workManager: WorkManager

    private lateinit var mobileViewModel: MobileViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager.getInstance(this)
        payIntentStore = PayIntentStore.getInstance(this)
        workManager = WorkManager.getInstance(this)

        mobileViewModel = ViewModelProvider(this)[MobileViewModel::class.java]

        val uploadWorkRequest: WorkRequest = OneTimeWorkRequestBuilder<VerifySubsWorker>()
            .build()
        WorkManager.getInstance(this).enqueue(uploadWorkRequest)

        setContent {
            OTheme {
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
                ) {
                    Column {
                        Button(onClick = {
                            SubsActivity.start(this@TestActivity)
                        }) {
                            Text(text = "Compose Paywall")
                        }

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

                        Button(onClick = {
                            InvoiceStore.getInstance(this@TestActivity)
                                .savePurchaseAction(PurchaseAction.BUY)
                            BuyerInfoActivity.start(this@TestActivity)
                        }) {
                            Text(text = "Address - Buy")
                        }

                        Button(onClick = {
                            AuthActivity.start(this@TestActivity)
                        }) {
                            Text(text = "Sign In/Up Activity")
                        }

                        Button(onClick = {
                            SignInFragment
                                .forEmailLogin()
                                .show(supportFragmentManager, "SignInFragment")
                        }) {
                            Text(text = "Login")
                        }

                        Button(onClick = {
                            SignUpFragment
                                .forEmailLogin()
                                .show(supportFragmentManager, "SignUpFragment")
                        }) {
                            Text(text = "Sign Up")
                        }
                        
                        Button(onClick = {
                            mobileViewModel.mobileLiveData.value = "1234567890"
                            SignInFragment
                                .forMobileLink().
                                show(supportFragmentManager, "TestMobileLinkExistingEmail")
                        }) {
                            Text(text = "Mobile Link Existing Email")
                        }
                        
                        Button(onClick = {
                            sessionManager.logout()
                            sessionManager.saveAccount(AccountBuilder()
                                .withTier(null)
                                .build())
                        }) {
                            Text(text = "Free User")
                        }
                        
                        Button(onClick = {
                            sessionManager.logout()
                            sessionManager.saveAccount(AccountBuilder()
                                .withAccountKind(LoginMethod.WECHAT)
                                .withTier(null)
                                .build())
                        }) {
                            Text(text = "Wx-only Free User")
                        }
                        
                        Button(onClick = {
                            sessionManager.logout()
                            sessionManager.saveAccount(AccountBuilder()
                                .build())
                        }) {
                            Text(text = "Standard User")
                        }
                        
                        Button(onClick = {
                            sessionManager.logout()
                            sessionManager.saveAccount(
                                AccountBuilder()
                                    .withTier(Tier.PREMIUM)
                                    .build()
                            )
                        }) {
                            Text(text = "Premium User")
                        }
                        
                        Button(onClick = {
                            sessionManager.logout()
                            sessionManager.saveAccount(
                                AccountBuilder()
                                    .withVip(true)
                                    .build()
                            )
                        }) {
                            Text(text = "VIP User")
                        }
                        
                        Button(onClick = {
                            sessionManager.logout()
                            sessionManager.saveAccount(
                                AccountBuilder()
                                    .withTier(Tier.STANDARD)
                                    .withPayMethod(PayMethod.STRIPE)
                                    .withAutoRenewal(true)
                                    .build())
                        }) {
                            Text(text = "Stripe Standard Year")
                        }
                        
                        Button(onClick = {
                            sessionManager.logout()
                            sessionManager.saveAccount(
                                AccountBuilder()
                                    .withPayMethod(PayMethod.STRIPE)
                                    .withAutoRenewal(true)
                                    .withCycle(Cycle.MONTH)
                                    .build()
                            )
                        }) {
                            Text(text = "Stripe Standard Month")
                        }
                        
                        Button(onClick = {
                            sessionManager.logout()
                            sessionManager.saveAccount(
                                AccountBuilder()
                                    .withPayMethod(PayMethod.STRIPE)
                                    .withAutoRenewal(true)
                                    .withTier(Tier.PREMIUM)
                                    .build()
                            )
                        }) {
                            Text(text = "Stripe Premium")
                        }
                        
                        Button(onClick = {
                            sessionManager.logout()
                            sessionManager.saveAccount(
                                AccountBuilder()
                                    .withPayMethod(PayMethod.STRIPE)
                                    .withAutoRenewal(false)
                                    .build()
                            )
                        }) {
                            Text(text = "Stripe Auto Renew Off")
                        }
                        
                        Button(onClick = {
                            sessionManager.logout()
                            sessionManager.saveAccount(
                                AccountBuilder()
                                    .withPayMethod(PayMethod.STRIPE)
                                    .withAutoRenewal(true)
                                    .withStdAddOn(30)
                                    .withPrmAddOn(366)
                                    .build()
                            )
                        }) {
                            Text(text = "Stripe Auto Renew Off with Add-on")
                        }
                        
                        Button(onClick = {
                            sessionManager.logout()
                            sessionManager.saveAccount(
                                AccountBuilder()
                                    .withPayMethod(PayMethod.APPLE)
                                    .withAutoRenewal(true)
                                    .build()
                            )
                        }) {
                            Text(text = "IAP Premium")
                        }
                        
                        Button(onClick = {
                            sessionManager.logout()
                            sessionManager.saveAccount(
                                AccountBuilder()
                                    .withPayMethod(PayMethod.APPLE)
                                    .withAutoRenewal(false)
                                    .build()
                            )
                        }) {
                            Text(text = "IAP Standard")
                        }
                        
                        Button(onClick = {
                            sessionManager.logout()
                            sessionManager.saveAccount(
                                AccountBuilder()
                                    .withPayMethod(PayMethod.APPLE)
                                    .withAutoRenewal(false)
                                    .build()
                            )
                        }) {
                            Text(text = "IAP Auto Renew Off")
                        }

                        Button(onClick = {
                            sessionManager.logout()
                            sessionManager.saveAccount(
                                AccountBuilder()
                                    .withPayMethod(PayMethod.APPLE)
                                    .withAutoRenewal(true)
                                    .withStdAddOn(31)
                                    .withPrmAddOn(366)
                                    .build()
                            )
                        }) {
                            Text(text = "IAP AddOn")
                        }

                        Button(onClick = {
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
                        }) {
                            Text(text = "IAP Expired with AddOn")
                        }

                        Button(onClick = {
                            Log.i(TAG, "Subscribing to news topic")

                            FirebaseMessaging
                                .getInstance()
                                .subscribeToTopic("news")
                                .addOnCompleteListener {
                                    if (!it.isSuccessful) {
                                        alert(Appcompat, "Subscription failed").show()
                                    } else {
                                        alert(Appcompat, "Subscribed").show()
                                    }
                                }
                        }) {
                            Text(text = "FCM Subscribe a Topic")
                        }

                        Button(onClick = {
                            createPaymentReuslt()
                            LatestInvoiceActivity.start(this@TestActivity)
                        }) {
                            Text(text = "Show Payment Result")
                        }
                        
                        Button(onClick = {
                            createUpgrdeResult()
                            LatestInvoiceActivity.start(this@TestActivity)
                        }) {
                            Text(text = "Show Upgrade Result")
                        }

                        Button(onClick = { /*TODO*/ }) {
                            Text(text = "Service Acceptance")
                        }
                        
                        Button(onClick = {
                            ServiceAcceptance
                                .getInstance(this@TestActivity)
                                .clear()
                        }) {
                            Text(text = "Clear Service Acceptance")
                        }

                        Button(onClick = {
                            val request = OneTimeWorkRequestBuilder<VerifySubsWorker>()
                                .build()
                            workManager.enqueue(request)
                        }) {
                            Text(text = "One Time Work Manager")
                        }

                        Button(onClick = {
                            LatestInvoiceActivity
                                .start(this@TestActivity)
                        }) {
                            Text(text = "Latest Order Activity")
                        }

                        Button(onClick = {
                            createNotiChannel()
                        }) {
                            Text(text = "Create Notification Channel")
                        }
                        
                        Button(onClick = {
                            createNotification()
                        }) {
                            Text(text = "Create Local Notification")
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

    private fun createPaymentReuslt() {
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

    private fun createUpgrdeResult() {
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.article_top_menu, menu)

        return true
    }

    class AccountBuilder {

        private val ftcId = "0c726d53-2ec3-41e2-aa8c-5c4b0e23876a"
        private val unionId = "XiBumZN8QoFl4gO_gQS0ieqgjRU2"
        private val stripeCusId = "cus_Fo3kC8njfCdo"
        private val stripeSubId = "sub_65gBB0LBU3uD"
        private val iapTxId = "1000000799792245"
        private val licenceId = "lic_x0DaRxLHHuLG"
        private val mobile = "12345678901"

        private var accountKind: LoginMethod = LoginMethod.EMAIL
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
                    LoginMethod.EMAIL -> ftcId
                    LoginMethod.WECHAT -> ""
                    LoginMethod.MOBILE -> ftcId
                },
                unionId = when (accountKind) {
                    LoginMethod.EMAIL -> ""
                    LoginMethod.WECHAT -> unionId
                    LoginMethod.MOBILE -> ""
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
                wechat = if (accountKind == LoginMethod.WECHAT) {
                    Wechat(
                        nickname = "Wechat User",
                        avatarUrl = "https://randomuser.me/api/portraits/thumb/women/7.jpg"
                    )
                } else {
                    Wechat()
                },
                membership = buildMembership()
            )
        }
    }

    companion object {

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, TestActivity::class.java))
        }

        @JvmStatic
        fun newIntent(context: Context): Intent {
            return Intent(context, TestActivity::class.java)
        }
    }
}
