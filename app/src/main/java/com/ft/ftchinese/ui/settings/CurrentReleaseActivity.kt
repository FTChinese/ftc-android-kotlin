package com.ft.ftchinese.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityCurrentReleaseBinding
import com.ft.ftchinese.model.AppRelease
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.viewmodel.SettingsViewModel
import com.ft.ftchinese.viewmodel.SettingsViewModelFactory
import kotlinx.android.synthetic.main.simple_toolbar.*

/**
 * Show the release log of current version.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class CurrentReleaseActivity : ScopedAppActivity() {

    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var binding: ActivityCurrentReleaseBinding
    private lateinit var cache: FileCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_current_release)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        supportFragmentManager.commit {
            replace(R.id.release_detail, ReleaseLogFragment.newInstance())
        }

        cache = FileCache(this)

        setupViewModel()
    }

    // Get release data from cache, and fallback to server if not found.
    private fun setupViewModel() {
        settingsViewModel = ViewModelProvider(
            this,
            SettingsViewModelFactory(FileCache(this))
        ).get(SettingsViewModel::class.java)

        // Network status
        connectionLiveData.observe(this) {
            settingsViewModel.isNetworkAvailable.value = it
        }
        settingsViewModel.isNetworkAvailable.value = isConnected

        // First tries to load release from cache. If not found, fetch from network.
        settingsViewModel.cachedReleaseFound.observe(this, Observer {
            if (it) {
                return@Observer
            }

            // If cached data is not found, start fetching data from server.
            binding.inProgress = true
            settingsViewModel.fetchRelease(true)
        })

        settingsViewModel.releaseResult.observe(this) {
            binding.inProgress = false
        }

        // Start fetching data from cache.
        // The cache file is named after BuildConfig.VERSION_CODE.
        settingsViewModel.loadCachedRelease(AppRelease.currentCacheFile())
    }

    companion object {
        @JvmStatic

        fun start(context: Context?) {
            context?.startActivity(Intent(context, CurrentReleaseActivity::class.java))
        }
    }
}
