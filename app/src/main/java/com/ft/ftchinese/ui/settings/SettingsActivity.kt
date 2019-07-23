package com.ft.ftchinese.ui.settings

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import com.ft.ftchinese.R
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.simple_toolbar.*

// Reference: https://developer.android.com/guide/topics/ui/settings
@kotlinx.coroutines.ExperimentalCoroutinesApi
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        supportFragmentManager.beginTransaction()
                .replace(R.id.preference_fragment, PreferenceFragment.newInstance())
                .commit()

        btn_check_version.setOnClickListener {
            download()
        }
    }

    private fun download() {
        val req = DownloadManager.Request(Uri.parse("http://creatives.ftacademy.cn/minio/android/ftchinese-v2.1.2-ftc-release.apk"))
        req.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
        req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        req.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "com.ftchinese")
        req.setTitle("android.apk")
        req.setDescription("Open it after downloaded")
        req.setMimeType("application/vnd.android.package-archive")

        val dm: DownloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = dm.enqueue(req)

        val query = DownloadManager.Query().setFilterById(downloadId)
        val c = dm.query(query)
        if (c != null) {

        }

    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, SettingsActivity::class.java))
        }
    }
}
