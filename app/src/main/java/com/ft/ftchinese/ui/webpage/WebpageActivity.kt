package com.ft.ftchinese.ui.webpage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.browser.customtabs.CustomTabsIntent
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityWebpageBinding
import com.ft.ftchinese.ui.base.ScopedAppActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class WebpageActivity : ScopedAppActivity() {

    private lateinit var wpViewModel: WebpageViewModel
    private lateinit var binding: ActivityWebpageBinding
    private var pageMeta: WebpageMeta? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_webpage)
        setSupportActionBar(binding.toolbar.toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        pageMeta = intent.getParcelableExtra(EXTRA_URL)

        supportFragmentManager.commit {
            replace(R.id.web_view_holder, WebpageFragment.newInstance())
        }

        wpViewModel = ViewModelProvider(this)
            .get(WebpageViewModel::class.java)

        pageMeta?.let {
            supportActionBar?.title = it.title
            wpViewModel.urlLiveData.value = it.url
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (pageMeta?.showMenu == true) {
            menuInflater.inflate(R.menu.webpage_menu, menu)
        }
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
        pageMeta?.let {
            CustomTabsIntent
                .Builder()
                .build()
                .launchUrl(
                    this,
                    Uri.parse(it.url)
                )
        }
    }

    companion object {
        private const val EXTRA_URL = "extra_url"
        fun start(context: Context, meta: WebpageMeta) {
            val intent = Intent(context, WebpageActivity::class.java).apply {
                putExtra(EXTRA_URL, meta)

            }
            context.startActivity(intent)
        }
    }
}
