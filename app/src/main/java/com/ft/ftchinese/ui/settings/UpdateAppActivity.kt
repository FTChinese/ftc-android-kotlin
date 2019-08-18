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
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityUpdateAppBinding
import com.ft.ftchinese.model.AppRelease
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isNetworkConnected
import com.ft.ftchinese.ui.base.parseException
import com.ft.ftchinese.util.RequestCode
import kotlinx.android.synthetic.main.activity_update_app.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.Appcompat
import java.io.File

private const val PREF_FILE_DOWNLOAD = "app_download"
private const val PREF_DOWNLOAD_ID = "download_id"

@kotlinx.coroutines.ExperimentalCoroutinesApi
class UpdateAppActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var settingsViewModel: SettingsViewModel

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
                    enableButton(true)
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

                btn_start_download.text = getString(R.string.btn_install)
                enableButton(true)

                btn_start_download.setOnClickListener {


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

        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        binding.release = AppRelease()

        registerReceiver(onNotificationClicked, IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED))
        registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        settingsViewModel = ViewModelProvider(this)
                .get(SettingsViewModel::class.java)

        settingsViewModel.releaseResult.observe(this, Observer{
            onLatestRelease(it)
        })


        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)
            return
        }

        showProgress(true)
        toast(R.string.checking_latest_release)

        settingsViewModel.checkLatestRelease()
    }

    private fun onLatestRelease(result: ReleaseResult?) {

        showProgress(false)

        if (result == null) {
            toast(R.string.release_not_found)
            return
        }

        if (result.error != null) {
            toast(result.error)
            return
        }

        if (result.exception != null) {
            toast(parseException(result.exception))
            return
        }

        if (result.success == null) {
            toast(R.string.release_not_found)
            return
        }

        release = result.success
        binding.release = release

        if (!result.success.isNew) {
            return
        }

        supportFragmentManager.commit {
            replace(R.id.release_detail, ReleaseLogFragment.newInstance())
        }

        btn_start_download.setOnClickListener {
            if (requestPermission()) {
                enableButton(false)
                showProgress(true)
                startDownload(release)
            }
        }
    }

    private fun requestPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            enableButton(false)

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

                    enableButton(false)
                    showProgress(true)
                    startDownload(release)

                } else {
                    enableButton(false)
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

        info("File name $fileName")

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

        showProgress(false)

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

            info("Downloaded file $localUri")

            Uri.parse(localUri)
        } catch (e: Exception) {
            null
        } finally {
            c.close()
        }
    }

    private fun install(file: File) {
        val contentUri = getContentUri(file)

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

    private fun showProgress(show: Boolean) {
        progress_bar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun enableButton(enable: Boolean) {
        btn_start_download.isEnabled = enable
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onNotificationClicked)
        unregisterReceiver(onDownloadComplete)
    }

    companion object {
        @JvmStatic
        fun start(context: Context?) {
            val intent = Intent(context, UpdateAppActivity::class.java)
            context?.startActivity(intent)
        }
    }
}
