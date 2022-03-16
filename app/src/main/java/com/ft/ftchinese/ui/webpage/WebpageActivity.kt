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
import com.ft.ftchinese.model.legal.WebpageMeta
import com.ft.ftchinese.ui.base.ScopedAppActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class WebpageActivity : ScopedAppActivity() {

    private lateinit var wvViewModel: WVViewModel
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

        pageMeta = intent.getParcelableExtra(EXTRA_WEB_META)

        supportFragmentManager.commit {
            replace(R.id.web_view_holder, WebpageFragment.newInstance())
        }

        wvViewModel = ViewModelProvider(this)[WVViewModel::class.java]

        pageMeta?.let {
            supportActionBar?.title = it.title
            wvViewModel.urlLiveData.value = it.url
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (pageMeta?.showMenu == true) {
            menuInflater.inflate(R.menu.webpage_menu, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
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
        private const val EXTRA_WEB_META = "extra_webpage_meta"
        fun start(context: Context, meta: WebpageMeta) {
            val intent = Intent(context, WebpageActivity::class.java).apply {
                putExtra(EXTRA_WEB_META, meta)

            }
            context.startActivity(intent)
        }
    }
}
