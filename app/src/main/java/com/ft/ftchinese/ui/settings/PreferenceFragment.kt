package com.ft.ftchinese.ui.settings

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.database.ArticleDb
import com.ft.ftchinese.database.ReadingHistoryDao
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.viewmodel.SettingsViewModel
import com.ft.ftchinese.viewmodel.SettingsViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import org.jetbrains.anko.support.v4.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class PreferenceFragment : PreferenceFragmentCompat(),
        CoroutineScope by MainScope() {

    private var prefClearCache: Preference? = null
    private var prefClearHistory: Preference? = null
    private var prefCheckVersion: Preference? = null
    private var prefNotification: Preference? = null

    private lateinit var cache: FileCache
    private lateinit var readingHistoryDao: ReadingHistoryDao

    private lateinit var settingsViewModel: SettingsViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)

        cache = FileCache(context)
        readingHistoryDao = ArticleDb.getInstance(context).readDao()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        settingsViewModel = ViewModelProvider(
            this,
            SettingsViewModelFactory(cache)
        ).get(SettingsViewModel::class.java)

        settingsViewModel.calculateCacheSize()
        settingsViewModel.countReadArticles(readingHistoryDao)

        super.onCreate(savedInstanceState)
    }

    // onCreatePreferences is called by onCreate.
    // Initialize any variable before super.onCreate
    // is called.
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        prefClearCache = findPreference("pref_clear_cache")
        prefClearHistory = findPreference("pref_clear_history")
        prefNotification = findPreference("pref_notification")
        prefCheckVersion = findPreference("pref_check_version")

        prefCheckVersion?.summary = getString(R.string.current_version, BuildConfig.VERSION_NAME)

        // Show cache size.
        settingsViewModel.cacheSizeResult.observe(this) {
            prefClearCache?.summary = it
        }

        // Wait for cache clearing result.
        settingsViewModel.cacheClearedResult.observe(this) {

            // After cache is cleared, re-calculate cache size.
            settingsViewModel.calculateCacheSize()

            if (it) {
                toast(R.string.prompt_cache_cleared)
            } else {
                toast(R.string.prompt_cache_not_cleared)
            }
        }


        // Show how many articles user read.
        settingsViewModel.articlesReadResult.observe(this) {
            prefClearHistory?.summary = getString(R.string.summary_articles_read, it)
        }

        // Show result of clearing reading history.
        settingsViewModel.articlesClearedResult.observe(this) {
            if (it) {
                toast(R.string.prompt_reading_history)
            } else {
                toast("Cannot truncate your reading history")
            }
        }


        // Delete cached files
        prefClearCache?.setOnPreferenceClickListener {

            settingsViewModel.clearCache()
            true
        }

        // Clear reading history
        prefClearHistory?.setOnPreferenceClickListener {

            settingsViewModel.truncateReadArticles(readingHistoryDao)
            true
        }

        prefNotification?.setOnPreferenceClickListener {
            FCMActivity.start(context)

            true
        }

        // Show checking new version
        prefCheckVersion?.setOnPreferenceClickListener {
            UpdateAppActivity.start(context)

            true
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    companion object {
        fun newInstance() = PreferenceFragment()
    }
}
