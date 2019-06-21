package com.ft.ftchinese.ui.settings

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.util.FileCache
import com.ft.ftchinese.ui.article.ReadArticleViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.support.v4.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class PreferenceFragment : PreferenceFragmentCompat(),
        CoroutineScope by MainScope(),
        AnkoLogger {


    private var prefClearCache: Preference? = null
    private var prefClearHistory: Preference? = null
    //    private var mArticleStore: ArticleStore? = null
    private lateinit var cache: FileCache
    private lateinit var model: ReadArticleViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)

        cache = FileCache(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        model = ViewModelProviders.of(this).get(ReadArticleViewModel::class.java)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)

        prefClearCache = findPreference("pref_clear_cache")
        prefClearHistory = findPreference("pref_clear_history")
        findPreference<Preference>("pref_version_name")?.summary = BuildConfig.VERSION_NAME

        updateCacheUI()

        prefClearCache?.setOnPreferenceClickListener {

            val result = cache.clear()

            if (result) {
                toast(R.string.prompt_cache_cleared)
            } else {
                toast(R.string.prompt_cache_not_cleared)
            }

            updateCacheUI()
            true
        }

        prefClearHistory?.setOnPreferenceClickListener {

            launch {
                model.truncate()

                val totalItems = model.countRead()

                prefClearHistory?.summary = getString(R.string.summary_articles_read, totalItems)
                toast(R.string.prompt_reading_history)
            }

            true
        }
    }

    private fun updateCacheUI() {
        prefClearCache?.summary = cache.space()

        launch {
            val total = model.countRead()

            prefClearHistory?.summary = getString(R.string.summary_articles_read, total)
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
