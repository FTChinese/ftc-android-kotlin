package com.ft.ftchinese.ui.webpage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ft.ftchinese.model.content.WebpageMeta

class WebpageActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.getParcelableExtra<WebpageMeta>(EXTRA_WEB_META)
            ?.let {
                setContent {
                    WebpageScreen(
                        pageMeta = it
                    ) {
                        finish()
                    }
                }
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

