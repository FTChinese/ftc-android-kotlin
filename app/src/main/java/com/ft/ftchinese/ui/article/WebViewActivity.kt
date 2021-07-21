package com.ft.ftchinese.ui.article

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import androidx.databinding.DataBindingUtil
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityWebViewBinding
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

private const val EXTRA_WEB_URL = "extra_web_url"

/**
 * Used to load a webpage.
 */
class WebViewActivity : AppCompatActivity(), AnkoLogger {

    private lateinit var binding: ActivityWebViewBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_web_view)

        setSupportActionBar(binding.webToolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        val url = intent.getStringExtra(EXTRA_WEB_URL) ?: return

        binding.webContent.settings.apply {
            javaScriptEnabled = true
            loadsImagesAutomatically = true
            domStorageEnabled = true
            databaseEnabled = true
        }

        binding.webContent.apply {

            setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_BACK && binding.webContent.canGoBack()) {
                    binding.webContent.goBack()
                    return@setOnKeyListener true
                }
                false
            }
        }

        info("Loading url: $url")
        binding.webContent.loadUrl(url)
    }

    companion object {
        @JvmStatic
        fun start(context: Context?, url: String) {
            context?.startActivity(Intent(context, WebViewActivity::class.java).apply {
                putExtra(EXTRA_WEB_URL, url)
            })
        }
    }
}
