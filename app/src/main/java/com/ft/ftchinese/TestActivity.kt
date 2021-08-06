package com.ft.ftchinese

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.ft.ftchinese.databinding.ActivityTestBinding
import com.ft.ftchinese.model.content.ArticleType
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.model.enums.*
import com.ft.ftchinese.model.ftcsubs.ConfirmationParams
import com.ft.ftchinese.model.ftcsubs.Order
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.LoginMethod
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.reader.Wechat
import com.ft.ftchinese.model.stripesubs.Idempotency
import com.ft.ftchinese.service.VerifySubsWorker
import com.ft.ftchinese.store.InvoiceStore
import com.ft.ftchinese.store.OrderManager
import com.ft.ftchinese.store.ServiceAcceptance
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.about.LegalDetailsFragment
import com.ft.ftchinese.ui.article.ArticleActivity
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.checkout.BuyerInfoActivity
import com.ft.ftchinese.ui.checkout.LatestInvoiceActivity
import com.ft.ftchinese.ui.login.AuthActivity
import com.ft.ftchinese.ui.login.SignInFragment
import com.ft.ftchinese.ui.login.SignUpFragment
import com.ft.ftchinese.ui.login.WxExpireDialogFragment
import com.ft.ftchinese.ui.share.SocialShareFragment
import com.ft.ftchinese.ui.wxlink.LinkPreviewFragment
import com.ft.ftchinese.ui.wxlink.UnlinkActivity
import com.ft.ftchinese.ui.wxlink.WxEmailLink
import com.ft.ftchinese.wxapi.WXEntryActivity
import com.ft.ftchinese.wxapi.WXPayEntryActivity
import com.google.firebase.messaging.FirebaseMessaging
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.alert
import org.jetbrains.anko.appcompat.v7.Appcompat
import org.jetbrains.anko.info
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.toast
import org.threeten.bp.LocalDate
import org.threeten.bp.ZonedDateTime


@kotlinx.coroutines.ExperimentalCoroutinesApi
class TestActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var binding: ActivityTestBinding
    private lateinit var orderManger: OrderManager
    private lateinit var sessionManager: SessionManager
    private lateinit var workManager: WorkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_test)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        sessionManager = SessionManager.getInstance(this)
        orderManger = OrderManager.getInstance(this)
        workManager = WorkManager.getInstance(this)

        binding.addressBuy.onClick {
            InvoiceStore.getInstance(this@TestActivity)
                .savePurchaseAction(PurchaseAction.BUY)
            BuyerInfoActivity.start(this@TestActivity)
        }

        binding.addressRenew.onClick {
            InvoiceStore.getInstance(this@TestActivity)
                .savePurchaseAction(PurchaseAction.RENEW)
            BuyerInfoActivity.start(this@TestActivity)
        }

        binding.addressWinback.onClick {
            InvoiceStore.getInstance(this@TestActivity)
                .savePurchaseAction(PurchaseAction.WIN_BACK)
            BuyerInfoActivity.start(this@TestActivity)
        }

        binding.signInUp.onClick {
            AuthActivity.start(this@TestActivity)
        }

        binding.signIn.onClick {
            SignInFragment
                .forEmailLogin()
                .show(supportFragmentManager, "SignInFragment")
        }

        binding.signUp.onClick {
            SignUpFragment
                .formEmailLogin()
                .show(supportFragmentManager, "SignUpFragment")
        }

        binding.oneTimeWorkManager.setOnClickListener {
            val request = OneTimeWorkRequestBuilder<VerifySubsWorker>().build()
            workManager.enqueue(request)
        }

        val uploadWorkRequest: WorkRequest = OneTimeWorkRequestBuilder<VerifySubsWorker>()
            .build()

        WorkManager.getInstance(this).enqueue(uploadWorkRequest)

        binding.createChannel.setOnClickListener {
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

        binding.createNotification.setOnClickListener {
            createNotification()
        }

        intent.extras?.let {
            for (key in it.keySet()) {
                val value = intent.extras?.get(key)
                info("Key: $key Value: $value")
            }
        }

        // Subscribe a topic.
        binding.btnSubscribeTopic.setOnClickListener {
            info("Subscribing to news topic")

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
        }

        // Setup bottom bar menu
        binding.bottomBar.replaceMenu(R.menu.activity_test_menu)
        binding.bottomBar.setOnMenuItemClickListener {
            onBottomMenuItemClicked(it)

            true
        }

        binding.btnFreeUser.setOnClickListener {
            sessionManager.logout()
            sessionManager.saveAccount(Account(
                id = "0c726d53-2ec3-41e2-aa8c-5c4b0e23876a",
                unionId = null,
                stripeId = null,
                userName = "Free Edition",
                email = "free@example.org",
                isVerified = false,
                avatarUrl = null,
                loginMethod = LoginMethod.EMAIL,
                wechat = Wechat(
                    nickname = null,
                    avatarUrl = null
                ),
                membership = Membership()
            ))
        }

        binding.btnWxonlyUser.setOnClickListener {
            sessionManager.logout()
            sessionManager.saveAccount(Account(
                id = "",
                unionId = "0lwxee2KvHdym1FPhj1HdgIN7nW1",
                stripeId = null,
                userName = "Wx-only User",
                email = "",
                isVerified = false,
                avatarUrl = null,
                loginMethod = LoginMethod.WECHAT,
                wechat = Wechat(
                    nickname = "Marvin4296",
                    avatarUrl = "https://randomuser.me/api/portraits/thumb/men/10.jpg"
                ),
                membership = Membership()
            ))
        }

        binding.btnStandardUser.setOnClickListener {
            sessionManager.logout()
            sessionManager.saveAccount(Account(
                id = "0c726d53-2ec3-41e2-aa8c-5c4b0e23876a",
                unionId = null,
                stripeId = null,
                userName = "Standard Edition",
                email = "standard@example.org",
                isVerified = false,
                avatarUrl = null,
                loginMethod = LoginMethod.EMAIL,
                wechat = Wechat(
                    nickname = null,
                    avatarUrl = null
                ),
                membership = Membership(
                    tier = Tier.STANDARD,
                    cycle = Cycle.YEAR,
                    expireDate = LocalDate.now().plusYears(1),
                    payMethod = PayMethod.ALIPAY,
                    autoRenew = false,
                    status = null,
                    vip = false
                )
            ))
        }

        binding.btnPremiumUser.setOnClickListener {
            sessionManager.logout()
            sessionManager.saveAccount(Account(
                id = "0c726d53-2ec3-41e2-aa8c-5c4b0e23876a",
                unionId = null,
                stripeId = null,
                userName = "Premium Edition",
                email = "premium@example.org",
                isVerified = false,
                avatarUrl = null,
                loginMethod = LoginMethod.EMAIL,
                wechat = Wechat(
                    nickname = null,
                    avatarUrl = null
                ),
                membership = Membership(
                    tier = Tier.PREMIUM,
                    cycle = Cycle.YEAR,
                    expireDate = LocalDate.now().plusYears(1),
                    payMethod = PayMethod.ALIPAY,
                    autoRenew = false,
                    status = null,
                    vip = false
                )
            ))
        }

        binding.btnVipUser.setOnClickListener {
            sessionManager.logout()
            sessionManager.saveAccount(Account(
                id = "0c726d53-2ec3-41e2-aa8c-5c4b0e23876a",
                unionId = null,
                stripeId = null,
                userName = "Premium Edition",
                email = "premium@example.org",
                isVerified = false,
                avatarUrl = null,
                loginMethod = LoginMethod.EMAIL,
                wechat = Wechat(
                    nickname = null,
                    avatarUrl = null
                ),
                membership = Membership(
                    tier = null,
                    cycle = null,
                    expireDate = null,
                    payMethod = null,
                    autoRenew = false,
                    status = null,
                    vip = true
                )
            ))
        }

        binding.btnStripeStdYear.setOnClickListener {
            sessionManager.logout()
            sessionManager.saveAccount(Account(
                id = "0c726d53-2ec3-41e2-aa8c-5c4b0e23876a",
                unionId = null,
                stripeId = "cus_abc",
                userName = "Standard Yearly Edition",
                email = "standard@example.org",
                isVerified = false,
                avatarUrl = null,
                loginMethod = LoginMethod.EMAIL,
                wechat = Wechat(
                    nickname = null,
                    avatarUrl = null
                ),
                membership = Membership(
                    tier = Tier.STANDARD,
                    cycle = Cycle.YEAR,
                    expireDate = LocalDate.now().plusYears(1),
                    payMethod = PayMethod.STRIPE,
                    autoRenew = true,
                    status = StripeSubStatus.Active,
                    vip = false
                )
            ))
        }

        binding.btnStripeStdMonth.setOnClickListener {
            sessionManager.logout()
            sessionManager.saveAccount(Account(
                id = "0c726d53-2ec3-41e2-aa8c-5c4b0e23876a",
                unionId = null,
                stripeId = "cus_abc",
                userName = "Standard Edition",
                email = "standard@example.org",
                isVerified = false,
                avatarUrl = null,
                loginMethod = LoginMethod.EMAIL,
                wechat = Wechat(
                    nickname = null,
                    avatarUrl = null
                ),
                membership = Membership(
                    tier = Tier.STANDARD,
                    cycle = Cycle.MONTH,
                    expireDate = LocalDate.now().plusYears(1),
                    payMethod = PayMethod.STRIPE,
                    autoRenew = true,
                    status = StripeSubStatus.Active,
                    vip = false
                )
            ))
        }

        binding.btnStripePremium.setOnClickListener {
            sessionManager.logout()
            sessionManager.saveAccount(Account(
                id = "0c726d53-2ec3-41e2-aa8c-5c4b0e23876a",
                unionId = null,
                stripeId = "cus_abc",
                userName = "Standard Edition",
                email = "standard@example.org",
                isVerified = false,
                avatarUrl = null,
                loginMethod = LoginMethod.EMAIL,
                wechat = Wechat(
                    nickname = null,
                    avatarUrl = null
                ),
                membership = Membership(
                    tier = Tier.PREMIUM,
                    cycle = Cycle.YEAR,
                    expireDate = LocalDate.now().plusYears(1),
                    payMethod = PayMethod.STRIPE,
                    autoRenew = true,
                    status = StripeSubStatus.Active,
                    vip = false
                )
            ))
        }

        binding.btnStripeAutoRenewOff.setOnClickListener {
            sessionManager.logout()
            sessionManager.saveAccount(Account(
                id = "0c726d53-2ec3-41e2-aa8c-5c4b0e23876a",
                unionId = null,
                stripeId = "cus_abc",
                userName = "Auto Renew Off",
                email = "standard@example.org",
                isVerified = false,
                avatarUrl = null,
                loginMethod = LoginMethod.EMAIL,
                wechat = Wechat(
                    nickname = null,
                    avatarUrl = null
                ),
                membership = Membership(
                    tier = Tier.PREMIUM,
                    cycle = Cycle.YEAR,
                    expireDate = LocalDate.now().plusYears(1),
                    payMethod = PayMethod.STRIPE,
                    autoRenew = false,
                    status = StripeSubStatus.Active,
                    vip = false
                )
            ))
        }

        binding.btnStripeWithAddon.setOnClickListener {
            sessionManager.logout()
            sessionManager.saveAccount(Account(
                id = "0c726d53-2ec3-41e2-aa8c-5c4b0e23876a",
                unionId = null,
                stripeId = "cus_abc",
                userName = "Add On",
                email = "standard@example.org",
                isVerified = false,
                avatarUrl = null,
                loginMethod = LoginMethod.EMAIL,
                wechat = Wechat(
                    nickname = null,
                    avatarUrl = null
                ),
                membership = Membership(
                    tier = Tier.PREMIUM,
                    cycle = Cycle.YEAR,
                    expireDate = LocalDate.now().plusMonths(-1),
                    payMethod = PayMethod.STRIPE,
                    autoRenew = false,
                    status = StripeSubStatus.Canceled,
                    vip = false,
                    standardAddOn = 30,
                    premiumAddOn = 366,
                )
            ))
        }

        binding.btnIapStandard.setOnClickListener {
            sessionManager.logout()
            sessionManager.saveAccount(Account(
                id = "0c726d53-2ec3-41e2-aa8c-5c4b0e23876a",
                unionId = null,
                stripeId = null,
                userName = "IAP Standard",
                email = "standard@example.org",
                isVerified = false,
                avatarUrl = null,
                loginMethod = LoginMethod.EMAIL,
                wechat = Wechat(
                    nickname = null,
                    avatarUrl = null
                ),
                membership = Membership(
                    tier = Tier.STANDARD,
                    cycle = Cycle.YEAR,
                    expireDate = LocalDate.now().plusYears(1),
                    payMethod = PayMethod.APPLE,
                    autoRenew = true,
                    status = null,
                    appleSubsId = "1000000266289493",
                    vip = false
                )
            ))
        }

        binding.btnIapPremium.setOnClickListener {
            sessionManager.logout()
            sessionManager.saveAccount(Account(
                id = "0c726d53-2ec3-41e2-aa8c-5c4b0e23876a",
                unionId = null,
                stripeId = null,
                userName = "IAP Premium",
                email = "prmeium@example.org",
                isVerified = false,
                avatarUrl = null,
                loginMethod = LoginMethod.EMAIL,
                wechat = Wechat(
                    nickname = null,
                    avatarUrl = null
                ),
                membership = Membership(
                    tier = Tier.PREMIUM,
                    cycle = Cycle.YEAR,
                    expireDate = LocalDate.now().plusYears(1),
                    payMethod = PayMethod.APPLE,
                    autoRenew = true,
                    status = null,
                    appleSubsId = "1000000266289493",
                    vip = false
                )
            ))
        }

        binding.btnIapAutoRenewOff.setOnClickListener {
            sessionManager.logout()
            sessionManager.saveAccount(Account(
                id = "0c726d53-2ec3-41e2-aa8c-5c4b0e23876a",
                unionId = null,
                stripeId = null,
                userName = "IAP Premium",
                email = "prmeium@example.org",
                isVerified = false,
                avatarUrl = null,
                loginMethod = LoginMethod.EMAIL,
                wechat = Wechat(
                    nickname = null,
                    avatarUrl = null
                ),
                membership = Membership(
                    tier = Tier.PREMIUM,
                    cycle = Cycle.YEAR,
                    expireDate = LocalDate.now().plusYears(1),
                    payMethod = PayMethod.APPLE,
                    autoRenew = false,
                    status = null,
                    appleSubsId = "1000000266289493",
                    vip = false
                )
            ))
        }

        binding.btnPaymentResult.setOnClickListener {
            InvoiceStore.getInstance(this).saveInvoices(
                ConfirmationParams(
                    order = Order(
                        id = "order-id",
                        ftcId = "ftc-user-id",
                        unionId = null,
                        priceId = "price-id",
                        discountId = null,
                        price = 298.0,
                        tier = Tier.STANDARD,
                        cycle = Cycle.YEAR,
                        amount = 298.0,
                        kind = OrderKind.Create,
                        payMethod = PayMethod.ALIPAY,
                        createdAt = ZonedDateTime.now(),
                        confirmedAt = null,
                        startDate = null,
                        endDate =  null
                    ),
                    member = Membership()
                ).invoices
            )

            LatestInvoiceActivity.start(this)
        }

        binding.btnUpgradeResult.setOnClickListener {
            InvoiceStore.getInstance(this).saveInvoices(
                ConfirmationParams(
                    order = Order(
                        id = "order-id",
                        ftcId = "ftc-user-id",
                        unionId = null,
                        priceId = "standard-price-id",
                        discountId = null,
                        price = 1998.0,
                        tier = Tier.PREMIUM,
                        cycle = Cycle.YEAR,
                        amount = 1998.0,
                        kind = OrderKind.Upgrade,
                        payMethod = PayMethod.ALIPAY,
                        createdAt = ZonedDateTime.now(),
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

            LatestInvoiceActivity.start(this)
        }

        binding.btnMessageDialog.onClick {
//            MessageDialogFragment("This is a message dialog that can do nothing")
//                .show(supportFragmentManager, "TestMessageDialog")

            AlertDialog.Builder(this@TestActivity)
                .setMessage(R.string.mobile_link_taken)
                .setPositiveButton(R.string.action_done) { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }

        binding.bottomDialog.setOnClickListener {
            SocialShareFragment().show(supportFragmentManager, "TestBottomDialog")
        }

        binding.btnService.setOnClickListener {
            supportFragmentManager.commit {
                add(android.R.id.content, LegalDetailsFragment.newInstance())
            }
        }

        binding.btnLatestOrder.setOnClickListener {
            LatestInvoiceActivity.start(this)
        }

        binding.clearServiceAccepted.setOnClickListener {
            ServiceAcceptance.getInstance(this).clear()
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

    private fun onBottomMenuItemClicked(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_test_wechat_expired -> {
                WxExpireDialogFragment().show(supportFragmentManager, "WxExpiredDialog")
            }
            R.id.menu_test_anko_alert -> {
                alert(Appcompat,"Anko alert", "Test") {
                    positiveButton("OK") {

                    }
                    negativeButton("Cancel") {

                    }
                }.show()

            }
            R.id.menu_show_unlink_activity -> {
                UnlinkActivity.startForResult(this)
            }
            R.id.menu_wxpay_activity -> {
                // Create a mock order.
                // This order actually exists, since you
                // Wechat does not provide a fake test
                // mechanism.
                orderManger.save(Order(
                        id = "FTEFD5E11FDFA709E0",
                        tier = Tier.PREMIUM,
                        cycle = Cycle.YEAR,
                        amount = 1998.00,
                        kind = OrderKind.Create,
                        payMethod = PayMethod.WXPAY,
                        createdAt = ZonedDateTime.now()
                ))
                WXPayEntryActivity.start(this)
            }
            R.id.menu_wx_oauth -> {
                WXEntryActivity.start(this)
            }
            R.id.menu_link_preview -> {
                val current = sessionManager.loadAccount()
                if (current == null) {
                    toast("Not logged in yet")
                    return
                }
                LinkPreviewFragment(
                    WxEmailLink(
                        ftc = current,
                        wx = Account(
                            id = "",
                            unionId = "AgqiTngwsasF6r8m83jOdhZRolJ9",
                            stripeId = null,
                            userName = null,
                            email = "",
                            isVerified = false,
                            avatarUrl = null,
                            loginMethod = LoginMethod.WECHAT,
                            wechat = Wechat(
                                nickname = "aliquam_quas_minima",
                                avatarUrl = "https://randomuser.me/api/portraits/thumb/men/17.jpg"
                            ),
                            membership = Membership(
                                tier = Tier.STANDARD,
                                cycle = Cycle.YEAR,
                                expireDate = LocalDate.now().plusDays(30),
                                payMethod = PayMethod.WXPAY,
                                autoRenew = false,
                                status = null,
                                vip =  false
                            )
                        ),
                        loginMethod = LoginMethod.EMAIL,
                    )
                )
            }
            R.id.menu_clear_idempotency -> {
                Idempotency.getInstance(this).clear()
                toast("Cleared")
            }
            R.id.menu_stripe_subscription -> {

//                StripeSubActivity.startTest(this, findPlan(Tier.STANDARD, Cycle.YEAR))
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.article_top_menu, menu)

        return true
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
