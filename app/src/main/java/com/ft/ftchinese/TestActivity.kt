package com.ft.ftchinese

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.FileProvider
import androidx.core.content.edit
import com.ft.ftchinese.model.*
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.model.order.*
import com.ft.ftchinese.model.reader.*
import com.ft.ftchinese.model.subscription.Cycle
import com.ft.ftchinese.model.subscription.Tier
import com.ft.ftchinese.model.subscription.findPlan
import com.ft.ftchinese.service.PollService
import com.ft.ftchinese.ui.account.LinkPreviewActivity
import com.ft.ftchinese.ui.account.UnlinkActivity
import com.ft.ftchinese.ui.article.*
import com.ft.ftchinese.ui.login.WxExpireDialogFragment
import com.ft.ftchinese.ui.pay.LatestOrderActivity
import com.ft.ftchinese.ui.pay.StripeSubActivity
import com.ft.ftchinese.wxapi.WXEntryActivity
import com.ft.ftchinese.wxapi.WXPayEntryActivity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.android.synthetic.main.activity_test.*
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.Appcompat
import org.threeten.bp.LocalDate
import org.threeten.bp.ZonedDateTime
import java.io.File

@kotlinx.coroutines.ExperimentalCoroutinesApi
class TestActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var orderManger: OrderManager
    private var errorDialog: Dialog? = null
    private lateinit var sessionManager: SessionManager
    private val mockAccount = Account(
            id = "0c726d53-2ec3-41e2-aa8c-5c4b0e23876a",
            unionId = null,
            stripeId = null,
            userName = "Faker",
            email = "faker@example.org",
            isVerified = false,
            avatarUrl = null,
            loginMethod = LoginMethod.EMAIL,
            wechat = Wechat(
                    nickname = null,
                    avatarUrl = null
            ),
            membership = Membership(
                    id = "mmb_DYBOVDytt1PH",
                    tier = Tier.STANDARD,
                    cycle = Cycle.YEAR,
                    expireDate = LocalDate.now().plusYears(1),
                    payMethod = PayMethod.ALIPAY,
                    autoRenew = false,
                    status = null,
                    vip = false
            )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        sessionManager = SessionManager.getInstance(this)
        orderManger = OrderManager.getInstance(this)


        info("Internal directory of this app: $filesDir")
        info("Internal directory for this app's temporary cache files: $cacheDir")
        info("External files dir: ${getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)}")

        info("isExternalStorageWritable: ${isExternalStorageWritable()}")
        info("External storage directory: ${Environment.getExternalStorageDirectory()}")
        info("External Download directory: ${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}")

//        val i = PollService.newIntent(this)
//        startService(i)

//        PollService.setServiceAlarm(this, true)

        registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        registerReceiver(onDownloadClicked, IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED))

        btn_start_download.setOnClickListener {
            it.isEnabled = false
            toast("Start downloading")
            showProgress(true)
            download()
        }

        btn_find_download.setOnClickListener {
            getDownloadedUri()
        }

        btn_download_status.setOnClickListener {
            getDownloadStatus()
        }

        btn_install_apk.setOnClickListener {
            install()
        }

        create_channel.setOnClickListener {
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

        create_notification.setOnClickListener {
            createNotification()
        }

        intent.extras?.let {
            for (key in it.keySet()) {
                val value = intent.extras?.get(key)
                info("Key: $key Value: $value")
            }
        }

        // Subscribe a topic.
        subscribeButton.setOnClickListener {
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

        // Retrieve registration token.
        logTokenButton.setOnClickListener {
            FirebaseInstanceId
                    .getInstance()
                    .instanceId
                    .addOnCompleteListener(OnCompleteListener {
                        if (!it.isSuccessful) {
                            info("getInstanceId failed", it.exception)
                            alert(Appcompat, "Failed to get token due to: ${it.exception?.message}", "Failed").show()
                            return@OnCompleteListener
                        }

                        val token = it.result?.token

                        info("Token $token")
                        alert(Appcompat, "Token retrieved: $token", "Success").show()
                    })
        }

        check_google_api.setOnClickListener {
            if (checkPlayServices()) {
                alert(Appcompat, "Play service available").show()
            } else {
                alert(Appcompat, "Play service not available").show()
            }
        }

        download_google_play.setOnClickListener {
            val googleApiAvailability = GoogleApiAvailability.getInstance()

            googleApiAvailability.makeGooglePlayServicesAvailable(this)
                    .addOnSuccessListener {
                        toast("Success")
                    }
                    .addOnFailureListener {
                        toast("Failed")
                    }
        }

        bottom_bar.replaceMenu(R.menu.activity_test_menu)
        bottom_bar.setOnMenuItemClickListener {
            onBottomMenuItemClicked(it)

            true
        }

        show_bottom_sheet.setOnClickListener {

            BarrierFragment().show(supportFragmentManager, "BarrierFragment")
        }

        btn_login_in.setOnClickListener {
            sessionManager.saveAccount(mockAccount)
        }

    }

    private fun createNotification() {
        val intent = Intent(this, ArticleActivity::class.java).apply {
            putExtra(EXTRA_ARTICLE_TEASER, Teaser(
                    id = "001083331",
                    type = "story",
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

    private fun checkPlayServices(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)

        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                if (errorDialog == null) {
                    errorDialog = googleApiAvailability.getErrorDialog(this, resultCode, 2404)
                    errorDialog?.setCancelable(false)
                }

                if (errorDialog?.isShowing == false) {
                    errorDialog?.show()
                }
            }
        }

        return resultCode == ConnectionResult.SUCCESS
    }

    private val onDownloadClicked = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            info("intent: $intent")

            val downloadId = loadDownloadId()
            if (downloadId < 0) {
                return
            }

            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

            info("Notification clicked $id")

            if (downloadId == id) {
                install()
            }
        }
    }

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val downloadId = loadDownloadId()
            if (downloadId < 0) {
                return
            }

            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

            if (downloadId == id) {
                btn_start_download.isEnabled = true
                showProgress(false)

                alert(Appcompat,"Download complete. Install now?", "Install") {
                    positiveButton("Install") {
                        it.dismiss()

                        install()
                    }

                    negativeButton("Later") {
                        it.dismiss()
                    }
                }.show()

            }
        }
    }

    /**
     * @see https://stackoverflow.com/questions/39996491/open-downloaded-file-on-android-n-using-fileprovider
     * The path you passed to File must ba a connonical file
     * path with the `file://` part.
     */
    private fun install() {
        val localUri = getDownloadedUri()
        if (localUri == null) {
            toast("Downloaded file not found")
            return
        }

        info("Raw file path $localUri")

        val filePath = localUri.path
        info("File path: $filePath")

        if (filePath == null) {
            toast("Download file uri cannot be parsed")
            return
        }

        val downloadedFile = File(filePath)

        info("Downloaded file: $downloadedFile")

        try {

            startInstall(downloadedFile)

        } catch (e: Exception) {
            alert(Appcompat, "${e.message}", "Installation Failed") {
                positiveButton("Re-try") {
                    it.dismiss()
                    startInstall(downloadedFile)
                }
                positiveButton("Cancel") {

                    it.dismiss()
                }
            }.show()
        }
    }

    private fun startInstall(file: File) {
        val contentUri = FileProvider.getUriForFile(this, "com.ftchinese.fileprovider", file)

        info("file $contentUri")

        // Do not use ACTION_VIEW you found on most
        // stack overflow answers. It's too old.
        // Nor should you use ACTION_INSTALL_PACKAGE.
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            // The permission must be added, otherwise you
            // will get error parsing package.
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, applicationInfo.packageName)
        }

        startActivity(intent)
    }

    private fun download() {
        val parsedUri = Uri.parse("https://creatives.ftacademy.cn/minio/android/ftchinese-v3.0.0-ftc-release.apk")

        val fileName = parsedUri.lastPathSegment

        val req = DownloadManager.Request(parsedUri)
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setTitle("FT中文网${parsedUri.lastPathSegment}")
                .setMimeType("application/vnd.android.package-archive")

        val dm: DownloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val id = dm.enqueue(req)

        info("Download id: $id")

        saveDownloadId(id)
    }

    /**
     * Find the downloaded file.
     */
    private fun getDownloadedUri(): Uri? {
        val id = loadDownloadId()
        if (id < 0) {
            toast("Download id $id not found")
            return null
        }

        val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val query = DownloadManager.Query().setFilterById(id)
        val c = dm.query(query) ?: return null

        if (!c.moveToFirst()) {
            c.close()
            return null
        }


        return try {
            val localUri = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
            info("Downloaded file $localUri")
            c.close()

            return Uri.parse(localUri)
        } catch (e: Exception) {
            null
        } finally {
            c.close()
        }
    }

    private fun getDownloadStatus() {

        val id = loadDownloadId()
        if (id < 0) {
            return
        }

        val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val query = DownloadManager.Query().setFilterById(id)

        val c = dm.query(query) ?: return

        if (!c.moveToFirst()) {
            return
        }



        val status = try {
            c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
        } catch (e: Exception) {
            null
        } finally {
            c.close()
        } ?: return

        when (status) {
            DownloadManager.STATUS_PENDING -> {
                toast("Pending")
            }
            DownloadManager.STATUS_PAUSED -> {
                toast("Paused")
            }
            DownloadManager.STATUS_RUNNING -> {
                toast("Running")
            }
            DownloadManager.STATUS_SUCCESSFUL -> {
                toast("Successful")
            }
            DownloadManager.STATUS_FAILED -> {
                toast("Failed")
            }
        }
    }

    private fun saveDownloadId(id: Long) {
        getSharedPreferences(PREF_FILE_DOWNLOAD, Context.MODE_PRIVATE).edit {
            putLong(PREF_DOWNLOAD_ID, id)
        }
    }

    private fun loadDownloadId(): Long {
        return getSharedPreferences(PREF_FILE_DOWNLOAD, Context.MODE_PRIVATE).getLong(PREF_DOWNLOAD_ID, -1)
    }

    private fun showProgress(show: Boolean) {
        progress_bar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(PollService.ACTION_SHOW_NOTIFICATION)
        registerReceiver(onShowNotification, filter)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(onShowNotification)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onDownloadComplete)
        unregisterReceiver(onDownloadClicked)
    }

    private val onShowNotification = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            toast("Got a broadcast: ${intent?.action}")
        }
    }

    private fun onBottomMenuItemClicked(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_item_toggle_polling -> {
                val shouldStartAlarm = !PollService.isServiceAlarmOn(this)
                PollService.setServiceAlarm(this, shouldStartAlarm)
                invalidateOptionsMenu()

            }
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
                orderManger.save(Subscription(
                        id = "FTEFD5E11FDFA709E0",
                        tier = Tier.PREMIUM,
                        cycle = Cycle.YEAR,
                        cycleCount = 1,
                        extraDays = 1,
                        amount = 1998.00,
                        usageType = OrderUsage.CREATE,
                        payMethod = PayMethod.WXPAY,
                        createdAt = ZonedDateTime.now()
                ))
                WXPayEntryActivity.start(this)
            }
            R.id.menu_latest_order -> {
                LatestOrderActivity.start(this)
            }
            R.id.menu_wx_oauth -> {
                WXEntryActivity.start(this)
            }
            R.id.menu_link_preview -> {
                LinkPreviewActivity.startForResult(this, Account(
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
                                id = "mmb_bOWD32Qf3Xzd",
                                tier = Tier.STANDARD,
                                cycle = Cycle.YEAR,
                                expireDate = LocalDate.now().plusDays(30),
                                payMethod = PayMethod.WXPAY,
                                autoRenew = false,
                                status = null,
                                vip =  false
                        )
                ))
            }
            R.id.menu_clear_idempotency -> {
                Idempotency.getInstance(this).clear()
                toast("Cleared")
            }
            R.id.menu_stripe_subscripiton -> {

                StripeSubActivity.startTest(this, findPlan(Tier.STANDARD, Cycle.YEAR))
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.article_top_bar, menu)

//        menu?.findItem(R.id.menu_item_toggle_polling)
//                ?.title = if (PollService.isServiceAlarmOn(this)) "Stop polling" else "Start polling"

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_share -> {
                startActivity(Intent.createChooser(Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "FT中文网 - test")
                    type = "text/plain"
                }, "分享"))
            }
        }


        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val PREF_FILE_DOWNLOAD = "app_download"
        private const val PREF_DOWNLOAD_ID = "download_id"

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
