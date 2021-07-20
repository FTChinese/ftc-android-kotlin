package com.ft.ftchinese.ui.webabout

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.browser.customtabs.CustomTabsIntent
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityLegalDocBinding

class LegalDocActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLegalDocBinding
    private lateinit var url: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_legal_doc)
        setSupportActionBar(binding.toolbar.toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        intent.getStringExtra(EXTRA_URL)?.let {
            url = it
            supportFragmentManager.commit {
                replace(R.id.web_view_holder, WebpageFragment.newInstance(it))
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.webpage_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.open_in_browser -> {
                openInBrowser()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openInBrowser() {
        CustomTabsIntent
            .Builder()
            .build()
            .launchUrl(
                this,
                Uri.parse(this.url)
            )
    }

    companion object {
        private const val EXTRA_URL = "extra_url"
        fun start(context: Context, url: String) {
            val intent = Intent(context, LegalDocActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
            }
            context.startActivity(intent)
        }
    }
}
