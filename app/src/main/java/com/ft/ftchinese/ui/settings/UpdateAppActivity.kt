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
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityUpdateAppBinding
import com.ft.ftchinese.model.AppRelease
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.ui.components.ToastMessage
import com.ft.ftchinese.ui.dialog.AlertDialogFragment
import org.jetbrains.anko.alert
import org.jetbrains.anko.appcompat.v7.Appcompat
import org.jetbrains.anko.toast
import java.io.File

/**
 * Checking new release. If found, download and install it.
 * It requires those features combined to work:
 * - Broadcast
 * - Permission
 * - Storage: the downloaded file is store in shared storage. See https://developer.android.com/training/data-storage/shared/media
 */
class UpdateAppActivity : ScopedAppActivity() {

    private lateinit var appViewModel: UpdateAppViewModel
    private lateinit var binding: ActivityUpdateAppBinding
    private lateinit var downloadManager: DownloadManager

    // Handle user clicking from the notification bar.
    private val onNotificationClicked = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            val savedId = appViewModel.loadDownloadId()

            val clickedId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -2)

            if (clickedId == null || savedId != clickedId) {
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
                    initInstall(clickedId)
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

    private fun getDownloadStatus(id: Long): Int? {
        val query = DownloadManager.Query().setFilterById(id)
        val c = downloadManager.query(query) ?: return null

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

    // Handle download complete
    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val savedId = appViewModel.loadDownloadId()
            if (savedId < 0) {
                toast("Cannot locate download id")
                return
            }

            val notiId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -2)

            if (savedId == notiId) {
                alert(Appcompat, R.string.app_download_complete, R.string.title_install) {
                    positiveButton(R.string.btn_install) {
                        it.dismiss()
                        initInstall(notiId)
                    }

                    isCancelable = false

                    negativeButton(R.string.action_cancel) {
                        it.dismiss()
                    }

                }.show()

                binding.btnStartDownload.text = getString(R.string.btn_install)
                binding.btnStartDownload.isEnabled = true

                binding.btnStartDownload.setOnClickListener {
                    initInstall(savedId)
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

        // Broadcast intent action sent by the download manager when the user clicks on a running download,
        // either from a system notification or from the downloads UI.
        registerReceiver(onNotificationClicked, IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED))
        // Broadcast intent action sent by the download manager when a download completes.
        registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        setupViewModel()
    }

    private fun setupViewModel() {
        // Create view model.
        appViewModel = ViewModelProvider(this)[UpdateAppViewModel::class.java]

        appViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }

        appViewModel.refreshingLiveData.observe(this) {

        }

        appViewModel.toastLiveData.observe(this) {
            when (it) {
                is ToastMessage.Resource -> toast(it.id)
                is ToastMessage.Text -> toast(it.text)
            }
        }

        appViewModel.newReleaseLiveData.observe(this) { release ->
            if (release.versionCode <= BuildConfig.VERSION_CODE) {
                binding.alreadyLatest = true
                return@observe
            }

            binding.hasNewVersion = true
            binding.versionName = getString(R.string.found_new_release, release?.versionName)

            // Button to start download.
            binding.btnStartDownload.setOnClickListener {
                if (requestPermission()) {
                    binding.btnStartDownload.isEnabled = false
                    binding.inProgress = true
                    startDownload(release)
                }
            }
        }

        appViewModel.loadRelease()
    }

    private fun requestPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            binding.btnStartDownload.isEnabled = false

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                RequestCode.PERMISSIONS
            )
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

                    appViewModel.newReleaseLiveData.value?.let {
                        startDownload(it)
                    }


                } else {
                    binding.btnStartDownload.isEnabled = false
                }
            }
        }
    }

    private fun startDownload(release: AppRelease) {

        val req = buildDownloadRequest(
            context = this,
            release = release
        )

        if (req == null) {
            toast(R.string.download_not_found)
            return
        }

        val id = downloadManager.enqueue(req)

        appViewModel.saveDownloadId(id)

        binding.inProgress = false

        AlertDialogFragment
            .newMsgInstance(
                getString(R.string.wait_download_finish)
            )
            .show(supportFragmentManager, "AppDownloadStarted")
    }

    // Get the downloaded uri when we try to install it.
    private fun retrieveDownloadUri(id: Long): Uri? {

        val query = DownloadManager.Query().setFilterById(id)
        val c = downloadManager.query(query) ?: return null

        if (!c.moveToFirst()) {
            c.close()
            return null
        }

        return try {
            // Uri where downloaded file will be stored.
            val localUri = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)) ?: return null

            Uri.parse(localUri)
        } catch (e: Exception) {
            null
        } finally {
            c.close()
        }
    }

    private fun initInstall(id: Long) {

        val localUri = retrieveDownloadUri(id)
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

    private fun install(file: File) {
        val contentUri = buildContentUri(this, file)

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

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onNotificationClicked)
        unregisterReceiver(onDownloadComplete)
    }

    companion object {
        @JvmStatic
        fun newIntent(context: Context?): Intent {
            return Intent(
                context,
                UpdateAppActivity::class.java
            )
        }

        @JvmStatic
        fun start(context: Context?) {
            val intent = Intent(context, UpdateAppActivity::class.java)
            context?.startActivity(intent)
        }
    }
}

// Build download request after user clicked download button
private fun buildDownloadRequest(context: Context, release: AppRelease): DownloadManager.Request? {
    val parsedUri = Uri.parse(release.apkUrl)
    val fileName = parsedUri.lastPathSegment ?: return null

    return try {
        DownloadManager.Request(parsedUri)
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setTitle(context.getString(R.string.download_title, release.versionName))
            .setMimeType("application/vnd.android.package-archive")
    } catch (e: Exception) {
        null
    }
}

// Build file uri of downloaded file when we try to install it.
private fun buildContentUri(context: Context, file: File): Uri {
    return FileProvider
        .getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            file
        )
}
