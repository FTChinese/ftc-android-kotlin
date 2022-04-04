package com.ft.ftchinese.ui.settings

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityUpdateAppBinding
import com.ft.ftchinese.model.AppRelease
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.model.fetch.FetchResult
import org.jetbrains.anko.alert
import org.jetbrains.anko.appcompat.v7.Appcompat
import org.jetbrains.anko.toast
import java.io.File

private const val PREF_FILE_DOWNLOAD = "app_download"
private const val PREF_DOWNLOAD_ID = "download_id"
private const val EXTRA_CACHE_FILENAME = "extra_cache_filename"

class UpdateAppActivity : ScopedAppActivity() {

    private lateinit var appViewModel: UpdateAppViewModel

    private lateinit var binding: ActivityUpdateAppBinding
    private var release: AppRelease? = null

    // Handle user clicking from the notification bar.
    private val onNotificationClicked = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val savedId = loadDownloadId()
            if (savedId < 0) {
                toast("Cannot locate download id")
                return
            }

            val clickedId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -2)

            if (savedId != clickedId) {
                toast(R.string.download_not_found)
                return
            }

            when (getDownloadStatus(clickedId)) {
                DownloadManager.STATUS_PENDING -> {
                    toast(R.string.download_pending)
                }
                DownloadManager.STATUS_PAUSED -> {
                    toast(R.string.download_paused)
                }
                DownloadManager.STATUS_RUNNING -> {
                    toast(R.string.download_running)
                }
                DownloadManager.STATUS_SUCCESSFUL -> {
                    startInstall(clickedId)
                }
                DownloadManager.STATUS_FAILED -> {
                    toast(R.string.download_failed)
                    binding.btnStartDownload.isEnabled = true
                }
                else -> {
                    toast("Unknown status")
                }
            }
        }
    }

    // Handle download complete
    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val savedId = loadDownloadId()
            if (savedId < 0) {
                toast("Cannot locate download id")
                return
            }

            val notiId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -2)

            if (savedId == notiId) {
                alert(Appcompat, R.string.app_download_complete, R.string.title_install) {
                    positiveButton(R.string.btn_install) {
                        it.dismiss()
                        startInstall(notiId)
                    }

                    isCancelable = false

                    negativeButton(R.string.action_cancel) {
                        it.dismiss()
                    }

                }.show()

                binding.btnStartDownload.text = getString(R.string.btn_install)
                binding.btnStartDownload.isEnabled = true

                binding.btnStartDownload.setOnClickListener {
                    startInstall(savedId)
                }

            } else {
                toast(R.string.download_not_found)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_update_app)

        setSupportActionBar(binding.toolbar.toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        binding.inProgress = true

        registerReceiver(onNotificationClicked, IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED))
        registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        setupViewModel()
    }

    private fun setupViewModel() {
        // Create view model.
        appViewModel = ViewModelProvider(this)[UpdateAppViewModel::class.java]

        // Network status
        connectionLiveData.observe(this) {
            appViewModel.isNetworkAvailable.value = it
        }

        // Latest release log might already cached by LatestReleaseWorker.
        appViewModel.cachedReleaseFound.observe(this) {
            if (it) {
                return@observe
            }
            // If cache is not found, fetch from server.
            appViewModel.fetchRelease(current = false)
        }

        // The release might comes either from cache or from server.
        appViewModel.releaseResult.observe(this) {
            onLatestRelease(it)
        }

        // If coming from notification, the background worker should already saved the release log.
        // Load it from cache, and it not found, then fetching latest release from server.
        val filename = intent.getStringExtra(EXTRA_CACHE_FILENAME)

        if (filename != null) {
            // Load from cache.
            // If cache not found, the cachedReleaseFound observer will call checkLatestRelease
            appViewModel.loadCachedRelease(filename)
            return
        }

        // No coming from notification. Fetch data directly from server.
        toast(R.string.checking_latest_release)

        appViewModel.fetchRelease(current = false)
    }

    private fun onLatestRelease(result: FetchResult<AppRelease>) {

        binding.inProgress = false

        when (result) {
            is FetchResult.LocalizedError -> {
                toast(result.msgId)
            }
            is FetchResult.TextError -> {
                toast(result.text)
            }
            is FetchResult.Success -> {
                release = result.data

                if (!result.data.isNew) {
                    binding.alreadyLatest = true
                    return
                } else {
                    binding.hasNewVersion = true
                    binding.versionName = getString(R.string.found_new_release, release?.versionName)
                }

                // Button to start download.
                binding.btnStartDownload.setOnClickListener {
                    if (requestPermission()) {
                        binding.btnStartDownload.isEnabled = false
                        binding.inProgress = true
                        startDownload(release)
                    }
                }
            }
        }

    }

    private fun requestPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            binding.btnStartDownload.isEnabled = false

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            } else {
                ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        RequestCode.PERMISSIONS
                )
            }
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            RequestCode.PERMISSIONS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    binding.inProgress = true
                    binding.btnStartDownload.isEnabled = false
                    startDownload(release)

                } else {
                    binding.btnStartDownload.isEnabled = false
                }
            }
        }
    }

    private fun startDownload(release: AppRelease?) {
        if (release == null) {
            return
        }

        val parsedUri = Uri.parse(release.apkUrl)
        val fileName = parsedUri.lastPathSegment

        if (fileName == null) {
            toast(R.string.download_not_found)
            return
        }

        val req = DownloadManager.Request(parsedUri)
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setTitle(getString(R.string.download_title, release.versionName))
                .setMimeType("application/vnd.android.package-archive")

        val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val id = dm.enqueue(req)

        getSharedPreferences(PREF_FILE_DOWNLOAD, Context.MODE_PRIVATE).edit {
            putLong(PREF_DOWNLOAD_ID, id)
        }

        binding.inProgress = false

        alert(Appcompat, R.string.wait_download_finish) {
            positiveButton(R.string.action_ok) {
                it.dismiss()
            }
        }.show()
    }

    private fun getContentUri(file: File): Uri {
        return FileProvider
                .getUriForFile(
                        this,
                        "${BuildConfig.APPLICATION_ID}.fileprovider",
                        file
                )
    }

    private fun loadDownloadId(): Long {
        return getSharedPreferences(PREF_FILE_DOWNLOAD, Context.MODE_PRIVATE)
                .getLong(PREF_DOWNLOAD_ID, -1)
    }

    private fun startInstall(id: Long) {
        val localUri = getDownloadUri(id)
        if (localUri == null) {
            toast("Downloaded file not found")
            return
        }

        val filePath = localUri.path

        if (filePath == null) {
            toast("Download file uri cannot be parsed")
            return
        }

        val downloadedFile = File(filePath)

        try {
            install(downloadedFile)
        } catch (e: Exception) {
            alert(Appcompat, "${e.message}", "Installation Failed") {
                positiveButton("Re-try") {
                    it.dismiss()
                    install(downloadedFile)
                }
                positiveButton("Cancel") {

                    it.dismiss()
                }
            }.show()
        }
    }

    private fun getDownloadUri(id: Long): Uri? {
        val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val query = DownloadManager.Query().setFilterById(id)
        val c = dm.query(query) ?: return null

        if (!c.moveToFirst()) {
            c.close()
            return null
        }

        return try {
            val localUri = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)) ?: return null

            Uri.parse(localUri)
        } catch (e: Exception) {
            null
        } finally {
            c.close()
        }
    }

    private fun install(file: File) {
        val contentUri = getContentUri(file)

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

    private fun getDownloadStatus(id: Long): Int? {
        val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val query = DownloadManager.Query().setFilterById(id)
        val c = dm.query(query) ?: return null

        if (!c.moveToFirst()) {
            c.close()
            return null
        }

        return try {
            val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))

            status
        } catch (e: Exception) {
            null
        } finally {
            c.close()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onNotificationClicked)
        unregisterReceiver(onDownloadComplete)
    }

    companion object {
        @JvmStatic
        fun newIntent(context: Context?, filename: String): Intent {
            return Intent(
                context,
                UpdateAppActivity::class.java
            ).apply {
                putExtra(EXTRA_CACHE_FILENAME, filename)
            }
        }

        @JvmStatic
        fun start(context: Context?) {
            val intent = Intent(context, UpdateAppActivity::class.java)
            context?.startActivity(intent)
        }
    }
}
