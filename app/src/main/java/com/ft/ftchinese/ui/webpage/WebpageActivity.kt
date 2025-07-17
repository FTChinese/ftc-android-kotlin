package com.ft.ftchinese.ui.webpage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ft.ftchinese.model.content.WebpageMeta
import com.ft.ftchinese.ui.theme.OTheme
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.MaterialTheme
import com.ft.ftchinese.ui.theme.OColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.SideEffect

class WebpageActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge layout
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Use dark status bar icons (assuming light background)
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true

        intent.getParcelableExtra<WebpageMeta>(EXTRA_WEB_META)
            ?.let {
                setContent {
                    val navColor = OColor.wheat.toArgb()
                    SideEffect {
                        window.navigationBarColor = navColor
                    }
                    OTheme {
                        WebpageScreen(
                            pageMeta = it
                        ) {
                            finish()
                        }
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

