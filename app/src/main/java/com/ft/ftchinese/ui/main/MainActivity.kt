package com.ft.ftchinese.ui.main

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.model.content.WebpageMeta
import com.ft.ftchinese.service.LatestReleaseWorker
import com.ft.ftchinese.service.VerifySubsWorker
import com.ft.ftchinese.ui.webpage.WebpageActivity
import com.ft.ftchinese.viewmodel.ConversionViewModel
import com.ft.ftchinese.viewmodel.UserViewModel
import com.tencent.mm.opensdk.openapi.WXAPIFactory

class MainActivity : ComponentActivity() {

    private lateinit var conversionViewModel: ConversionViewModel
    private lateinit var userViewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        registerWx()

        setContent { 
            MainApp(userViewModel = userViewModel)
        }

        createNotificationChannel()
        setupWorker()
        setupConversion()
    }

    private fun createNotificationChannel() {
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
        }
    }

    private fun setupWorker() {
        val workManager = WorkManager.getInstance(this)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .build()

        val verifyWork = OneTimeWorkRequestBuilder<VerifySubsWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork("verifySubscription", ExistingWorkPolicy.REPLACE, verifyWork)

        workManager.getWorkInfoByIdLiveData(verifyWork.id).observe(this) { workInfo ->
            Log.i(TAG, "verifyWork state ${workInfo.state}")
        }

        val upgradeWork = OneTimeWorkRequestBuilder<LatestReleaseWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork("latestRelease", ExistingWorkPolicy.REPLACE, upgradeWork)
    }

    // Register Wechat id
    private fun registerWx() {
        WXAPIFactory.createWXAPI(
            this,
            BuildConfig.WX_SUBS_APPID, false
        ).apply {
            registerApp(BuildConfig.WX_SUBS_APPID)
        }
    }

    private fun setupConversion() {
        conversionViewModel = ViewModelProvider(this)[ConversionViewModel::class.java]
        // Open conversion tracking page.
        conversionViewModel.campaignLiveData.observe(this) {
            WebpageActivity.start(
                context = this,
                meta = WebpageMeta(
                    title = "",
                    url = it.url,
                    showMenu = false,
                )
            )
        }
        conversionViewModel.launchTask(3, 30, 7)
    }

    // Ensure ChannelPageScreen is updated in case user' account changed.
    override fun onResume() {
        super.onResume()

        userViewModel.reloadAccount()
    }

    companion object {
        private const val TAG = "MainActivity"

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, MainActivity::class.java))
        }
    }
}


