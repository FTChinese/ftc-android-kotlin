package com.ft.ftchinese.user

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.database.ArticleStore
import com.ft.ftchinese.util.Store
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.support.v4.toast

// Reference: https://developer.android.com/guide/topics/ui/settings
class SettingsActivity : SingleFragmentActivity() {
    override fun createFragment(): Fragment {
        return SettingsFragment.newInstance()
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

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)

        prefClearCache = findPreference("pref_clear_cache")
        prefClearHistory = findPreference("pref_clear_history")
        findPreference("pref_version_name").summary = BuildConfig.VERSION_NAME

        updateCacheUI()

        prefClearCache?.setOnPreferenceClickListener {

            val result = Store.clearFiles(context)
            if (result) {
                toast("缓存已清空")
            } else {
                toast("无法清空缓存")
            }

            updateCacheUI()
            true
        }

        prefClearHistory?.setOnPreferenceClickListener {
            val ctx = context ?: return@setOnPreferenceClickListener false
            toast("Clear reading history")
            ArticleStore.getInstance(ctx).dropHistory()
            prefClearHistory?.summary = getString(R.string.summary_articles_read, 0)
            true
        }


    }

    private fun updateCacheUI() {
        val space = Store.filesSpace(context)

        if (space != null) {
            prefClearCache?.summary = space
        }

        val ctx = context ?: return
        val total = ArticleStore.getInstance(ctx).countHistory()
        prefClearHistory?.summary = getString(R.string.summary_articles_read, total)
    }

    // Reference: https://developer.android.com/guide/topics/ui/settings#ReadingPrefs
//    override fun onResume() {
//        super.onResume()
//
//        preferenceScreen.sharedPreferences
//                .registerOnSharedPreferenceChangeListener(this)
//    }
//
//    override fun onPause() {
//        super.onPause()
//        preferenceScreen.sharedPreferences
//                .unregisterOnSharedPreferenceChangeListener(this)
//    }
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//
//    }

//    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
//        if (key == SettingsActivity.PREF_KEY_LANGUAGE) {
//
//        }
//    }

    companion object {
        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }
}