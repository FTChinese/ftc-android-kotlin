package com.ft.ftchinese.ui

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import com.ft.ftchinese.R
import com.ft.ftchinese.service.PollService
import kotlinx.android.synthetic.main.activity_test.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.toast

class TestActivity : AppCompatActivity() {

    private var downloadId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        btn_test_alert.setOnClickListener {
            AlertDialog.Builder(this)
                    .setTitle("Title")
                    .setMessage("Test alert dialog")
                    .setPositiveButton("OK") { dialog, which ->

                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel") { dialog, which ->
                        dialog.dismiss()
                    }
                    .create().show()
        }

//        val i = PollService.newIntent(this)
//        startService(i)

//        PollService.setServiceAlarm(this, true)

        registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        btn_check_version.setOnClickListener {
            download()
        }
    }

    private fun download() {
        val req = DownloadManager.Request(Uri.parse("https://creatives.ftacademy.cn/minio/android/ftchinese-v2.1.3-ftc-release.apk"))
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "com.ftchinese")
            .setTitle("FT中文网最新版安卓App")
            .setDescription("Open it after downloaded")
            .setMimeType("application/vnd.android.package-archive")

        val dm: DownloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = dm.enqueue(req)

    }

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

            if (downloadId == id) {
                toast("Download complete")
            }
        }
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
            else -> super.onOptionsItemSelected(item)
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
