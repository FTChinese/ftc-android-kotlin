package com.ft.ftchinese

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.content.edit
import androidx.fragment.app.commit
import com.ft.ftchinese.base.ScopedAppActivity
import com.ft.ftchinese.model.Account
import com.ft.ftchinese.model.LoginMethod
import com.ft.ftchinese.model.Membership
import com.ft.ftchinese.model.Wechat
import com.ft.ftchinese.model.order.*
import com.ft.ftchinese.service.PollService
import com.ft.ftchinese.ui.account.LinkPreviewActivity
import com.ft.ftchinese.ui.account.UnlinkActivity
import com.ft.ftchinese.ui.account.UnlinkAnchorFragment
import com.ft.ftchinese.ui.login.WxExpireDialogFragment
import com.ft.ftchinese.ui.pay.LatestOrderActivity
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.wxapi.WXEntryActivity
import com.ft.ftchinese.wxapi.WXPayEntryActivity
import kotlinx.android.synthetic.main.activity_test.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.alert
import org.jetbrains.anko.appcompat.v7.Appcompat
import org.jetbrains.anko.info
import org.jetbrains.anko.toast
import org.threeten.bp.LocalDate
import org.threeten.bp.ZonedDateTime
import java.io.File

@kotlinx.coroutines.ExperimentalCoroutinesApi
class TestActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var orderManger: OrderManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }


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

        registerReceiver(onNotificationClicked, IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED))

        btn_start_download.setOnClickListener {
            it.isEnabled = false
            toast("Start downloading")
            showProgress(true)
            download()
        }

        btn_find_download.setOnClickListener {
            findDownload()
        }

        btn_download_status.setOnClickListener {
            getDownloadStatus()
        }

    }

    private val onNotificationClicked = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            info("intent: $intent")
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

                alertInstall()

                toast("Download complete")
            }
        }
    }

    private fun alertInstall() {
        alert(Appcompat,"Download complete. Install now?", "Install") {
            positiveButton("Install") {
                it.dismiss()

                val fileUri = findDownload()
                if (fileUri == null) {
                    toast("Downloded file not found")
                }

                val promptInstall = Intent(Intent.ACTION_VIEW).
                        setDataAndType(Uri.parse(fileUri), "application/vnd.android.package-archive")

                startActivity(promptInstall)
            }
            negativeButton("Later") {
                it.dismiss()
            }
        }.show()
    }

    private fun download() {
        val req = DownloadManager.Request(Uri.parse("https://creatives.ftacademy.cn/minio/android/ftchinese-v2.1.3-ftc-release.apk"))
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "ftchinese-v2.1.3-ftc-release.apk")
                .setTitle("FT中文网App更新")
                .setMimeType("application/vnd.android.package-archive")

        val dm: DownloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val id = dm.enqueue(req)

        info("Download id: $id")

        saveDownloadId(id)
    }

    private fun findDownload(): String? {
        val id = loadDownloadId()
        if (id < 0) {
            toast("Download id $id not found")
            return null
        }

        val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val query = DownloadManager.Query().setFilterById(id)
        val c = dm.query(query)
        if (c != null) {
            if (c.moveToFirst()) {
                val fileUri = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))

                c.close()

                toast("Download file $fileUri")

                return fileUri
            }
        }

        c.close()

        return null
    }

    private fun getDownloadStatus() {

        val id = loadDownloadId()
        if (id < 0) {
            return
        }

        val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val query = DownloadManager.Query().setFilterById(id)

        val c = dm.query(query)

        if (c != null && c.moveToFirst()) {
            val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))

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

        c.close()
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

    private fun getPublicAlbumStorageDir(albumName: String): File? {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), albumName)
        if (!file.mkdir()) {
            info("Directory not created")
        }

        return file
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
        unregisterReceiver(onNotificationClicked)
    }

    private val onShowNotification = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            toast("Got a broadcast: ${intent?.action}")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.activity_test_menu, menu)
        menu
                ?.findItem(R.id.menu_item_toggle_polling)
                ?.title = if (PollService.isServiceAlarmOn(this)) "Stop polling" else "Start polling"

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.menu_item_toggle_polling -> {
                val shouldStartAlarm = !PollService.isServiceAlarmOn(this)
                PollService.setServiceAlarm(this, shouldStartAlarm)
                invalidateOptionsMenu()

                true
            }
            R.id.menu_test_wechat_expired -> {
                WxExpireDialogFragment().show(supportFragmentManager, "WxExpiredDialog")
                true
            }
            R.id.menu_test_anko_alert -> {
                alert(Appcompat,"Anko alert", "Test") {
                    positiveButton("OK") {

                    }
                    negativeButton("Cancel") {

                    }
                }.show()

                true
            }
            R.id.menu_show_unlink_activity -> {
                UnlinkActivity.startForResult(this, RequestCode.UNLINK)
                true
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
                true
            }
            R.id.menu_latest_order -> {
                LatestOrderActivity.start(this)
                true
            }
            R.id.menu_wx_oauth -> {
                WXEntryActivity.start(this)
                true
            }
            R.id.menu_link_preview -> {
                LinkPreviewActivity.startForResult(this, Account(
                        id = "",
                        unionId = "AgqiTngwsasF6r8m83jOdhZRolJ9",
                        stripeId = null,
                        userName = null,
                        email = "",
                        isVerified =  false,
                        avatarUrl = null,
                        isVip = false,
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
                                status = null
                        )
                ))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
