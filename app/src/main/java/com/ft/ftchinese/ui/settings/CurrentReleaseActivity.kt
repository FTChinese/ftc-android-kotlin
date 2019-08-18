package com.ft.ftchinese.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isNetworkConnected
import com.ft.ftchinese.util.FileCache
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class CurrentReleaseActivity : ScopedAppActivity() {

    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var cache: FileCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_current_release)

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
        settingsViewModel.cachedReleaseFound.observe(this, Observer {
            if (it) {
                return@Observer
            }

            if (!isNetworkConnected()) {
                toast(R.string.prompt_no_network)
                return@Observer
            }

            showProgress(true)

            settingsViewModel.fetchRelease(cache, BuildConfig.VERSION_NAME)
        })

        settingsViewModel.releaseResult.observe(this, Observer {
            showProgress(false)
        })

        showProgress(true)
        settingsViewModel.loadCachedRelease(cache)
    }

    private fun showProgress(show: Boolean) {
        progress_bar.visibility = if (show) View.VISIBLE else View.GONE
    }

    companion object {
        @JvmStatic

        fun start(context: Context?) {
            context?.startActivity(Intent(context, CurrentReleaseActivity::class.java))
        }
    }
}
