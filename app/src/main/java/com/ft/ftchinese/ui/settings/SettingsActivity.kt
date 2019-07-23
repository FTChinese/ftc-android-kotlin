package com.ft.ftchinese.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ft.ftchinese.R
import kotlinx.android.synthetic.main.simple_toolbar.*

// Reference: https://developer.android.com/guide/topics/ui/settings
@kotlinx.coroutines.ExperimentalCoroutinesApi
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        supportFragmentManager.beginTransaction()
                .replace(R.id.preference_fragment, PreferenceFragment.newInstance())
                .commit()
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, SettingsActivity::class.java))
        }
    }
}
