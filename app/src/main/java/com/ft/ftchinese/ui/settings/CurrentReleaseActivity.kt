package com.ft.ftchinese.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityCurrentReleaseBinding
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isNetworkConnected
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.viewmodel.SettingsViewModel
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.toast

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

        settingsViewModel = ViewModelProvider(this)
                .get(SettingsViewModel::class.java)

        cache = FileCache(this)

        supportFragmentManager.commit {
            replace(R.id.release_detail, ReleaseLogFragment.newInstance())
        }

        setup()
    }

    private fun setup() {
        // First tries to load release from cache. If not found, fetch from network.
        settingsViewModel.cachedReleaseFound.observe(this, Observer {
            if (it) {
                return@Observer
            }

            if (!isNetworkConnected()) {
                toast(R.string.prompt_no_network)
                return@Observer
            }

            binding.inProgress = true

            settingsViewModel.fetchRelease(cache, BuildConfig.VERSION_NAME)
        })

        settingsViewModel.releaseResult.observe(this, Observer {
            binding.inProgress = false
        })

        settingsViewModel.loadCachedRelease(cache)
    }

    companion object {
        @JvmStatic

        fun start(context: Context?) {
            context?.startActivity(Intent(context, CurrentReleaseActivity::class.java))
        }
    }
}
