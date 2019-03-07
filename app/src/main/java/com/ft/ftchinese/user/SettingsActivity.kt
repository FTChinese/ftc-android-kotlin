package com.ft.ftchinese.user

import android.content.Context
import android.content.Intent

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.database.ArticleStore
import com.ft.ftchinese.util.FileCache
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.support.v4.toast

// Reference: https://developer.android.com/guide/topics/ui/settings
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment_single)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        supportFragmentManager.beginTransaction()
                .replace(R.id.single_frag_holder, SettingsFragment.newInstance())
                .commit()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, SettingsActivity::class.java)
            context.startActivity(intent)
        }
    }
}

class SettingsFragment : PreferenceFragmentCompat(), AnkoLogger {


    private var prefClearCache: Preference? = null
    private var prefClearHistory: Preference? = null
    private var mArticleStore: ArticleStore? = null
    private var mFileCache: FileCache? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        mArticleStore = ArticleStore.getInstance(context)
        mFileCache = FileCache(context)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)

        prefClearCache = findPreference("pref_clear_cache")
        prefClearHistory = findPreference("pref_clear_history")
        findPreference<Preference>("pref_version_name").summary = BuildConfig.VERSION_NAME

        updateCacheUI()

        prefClearCache?.setOnPreferenceClickListener {

            val result = mFileCache?.clear() ?: return@setOnPreferenceClickListener true

            if (result) {
                toast(R.string.prompt_cache_cleared)
            } else {
                toast(R.string.prompt_cache_not_cleared)
            }

            updateCacheUI()
            true
        }

        prefClearHistory?.setOnPreferenceClickListener {

            GlobalScope.launch(Dispatchers.Main) {
                val ok = mArticleStore?.truncateHistory() ?: false

                if (ok) {
                    prefClearHistory?.summary = getString(R.string.summary_articles_read, 0)
                    toast(R.string.prompt_reading_history)
                } else {
                    toast("Cannot delete reading history now. Please retry later")
                }
            }

            true
        }
    }

    private fun updateCacheUI() {
        val space = mFileCache?.space()

        if (space != null) {
            prefClearCache?.summary = space
        }

        val total = mArticleStore?.countHistory() ?: 0
        prefClearHistory?.summary = getString(R.string.summary_articles_read, total)
    }

    companion object {
        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }
}