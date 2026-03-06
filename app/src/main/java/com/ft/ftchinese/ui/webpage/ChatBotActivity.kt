package com.ft.ftchinese.ui.webpage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.repository.HostConfig
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.store.SessionTokenStore
import com.ft.ftchinese.store.TokenManager
import com.ft.ftchinese.ui.theme.OColor
import com.ft.ftchinese.ui.theme.OTheme
import androidx.compose.ui.graphics.toArgb
import java.util.UUID

class ChatBotActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true

        val requestHeaders = buildChatBootstrapHeaders()

        setContent {
            val navColor = OColor.wheat.toArgb()
            SideEffect {
                window.navigationBarColor = navColor
            }
            OTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) {
                    ChatBotActivityScreen(
                        requestHeaders = requestHeaders,
                        onClose = { finish() }
                    )
                }
            }
        }
    }

    private fun buildChatBootstrapHeaders(): Map<String, String> {
        val traceId = UUID.randomUUID().toString()
        val headers = linkedMapOf<String, String>()
        headers["X-Client-Type"] = "android"
        headers["X-Client-Version"] = BuildConfig.VERSION_NAME
        headers["X-Chat-Trace-Id"] = traceId

        val deviceId = runCatching {
            TokenManager.getInstance(this).getToken()
        }.getOrNull()
        if (!deviceId.isNullOrBlank()) {
            headers["X-Device-Id"] = deviceId
        }

        val userId = runCatching {
            SessionManager.getInstance(this).loadAccount(raw = true)?.id
        }.getOrNull()
        if (!userId.isNullOrBlank()) {
            headers["X-User-Id"] = userId
        }

        val sessionToken = runCatching {
            SessionTokenStore.getInstance(this).load()
        }.getOrNull()
        if (!sessionToken.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $sessionToken"
        }

        return headers
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, ChatBotActivity::class.java))
        }
    }
}

@Composable
private fun ChatBotActivityScreen(
    requestHeaders: Map<String, String>,
    onClose: () -> Unit,
) {
    WebTabScreen(
        url = HostConfig.chatBootstrapUrl,
        title = "ChatFTC",
        requestHeaders = requestHeaders,
        onClose = onClose
    )
}
