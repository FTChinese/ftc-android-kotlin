package com.ft.ftchinese.ui.webpage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.ft.ftchinese.model.content.TemplateBuilder
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.repository.HostConfig
import com.ft.ftchinese.store.FileStore
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.store.WebAccessTokenStore
import com.ft.ftchinese.ui.theme.OColor
import com.ft.ftchinese.ui.theme.OTheme
import androidx.compose.ui.graphics.toArgb
import com.ft.ftchinese.ui.web.FtcWebView
import com.google.accompanist.web.rememberWebViewStateWithHTMLData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ChatBotActivity : ComponentActivity() {

    companion object {
        private const val TAG = "ChatBotAuth"
        private const val CHAT_FTC_PATH = "powertranslate/chat.html#webview=ftcapp"
        private const val ACCESS_TOKEN_REFRESH_SKEW_SECONDS = 300L
        private val PREMIUM_ACCESS_TOKEN_ROLES = setOf(
            "premium",
            "admin",
            "dev",
            "editor",
            "marketing",
            "bd",
        )
        private val ANDROID_CHAT_SCRIPT = """
            <script>
            document.documentElement.classList.add('is-android-app');
            </script>
        """.trimIndent()

        fun start(context: Context) {
            context.startActivity(Intent(context, ChatBotActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true

        setContent {
            var chatBundle by remember { mutableStateOf<ChatBundle?>(null) }
            val navColor = OColor.wheat.toArgb()

            LaunchedEffect(Unit) {
                chatBundle = withContext(Dispatchers.IO) {
                    prepareChatBundle()
                }
            }

            SideEffect {
                window.navigationBarColor = navColor
            }

            OTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) {
                    val bundle = chatBundle
                    if (bundle == null) {
                        WebContentLayout(
                            title = "ChatFTC",
                            loading = true,
                            onClose = { finish() }
                        ) {}
                    } else {
                        ChatBotActivityScreen(
                            html = bundle.html,
                            baseUrl = bundle.baseUrl,
                            onClose = { finish() }
                        )
                    }
                }
            }
        }
    }

    private fun prepareChatBundle(): ChatBundle {
        val account = prepareChatAccount()
        val chatUrl = "${HostConfig.canonicalUrl.trimEnd('/')}/$CHAT_FTC_PATH"
        val chatHtml = TemplateBuilder(FileStore(this).readChatTemplate())
            .withUserInfo(account)
            .withJs(ANDROID_CHAT_SCRIPT)
            .render()
        Log.i(TAG, "Opening bundled chat.html with baseUrl=$chatUrl")

        return ChatBundle(
            html = chatHtml,
            baseUrl = chatUrl,
        )
    }

    private fun prepareChatAccount(): Account? {
        val sessionManager = SessionManager.getInstance(this)
        val account = sessionManager.loadAccount(raw = true) ?: return null

        if (!shouldRefreshWebAccessToken(account)) {
            return account
        }

        return runCatching {
            AccountRepo.refresh(account)?.also { refreshed ->
                sessionManager.saveAccount(refreshed)
            }
        }.onSuccess { refreshed ->
            if (refreshed == null) {
                Log.w(TAG, "Chat accessToken refresh returned no account; using stored account")
            } else {
                Log.i(TAG, "Refreshed account before opening bundled ChatFTC")
            }
        }.onFailure {
            Log.w(TAG, "Failed to refresh account before opening bundled ChatFTC", it)
        }.getOrNull() ?: account
    }

    private fun shouldRefreshWebAccessToken(account: Account): Boolean {
        val token = WebAccessTokenStore.getInstance(this).load()
        if (token.isNullOrBlank()) {
            Log.i(TAG, "Missing web accessToken before ChatFTC; refreshing account")
            return true
        }

        if (isJwtExpiringSoon(token)) {
            Log.i(TAG, "Stored web accessToken is expired or expiring soon; refreshing account")
            return true
        }

        if (account.membership.webPrivilegeTier == Tier.PREMIUM && !hasPremiumAccessTokenRole(token)) {
            Log.i(TAG, "Stored web accessToken role is not premium before ChatFTC; refreshing account")
            return true
        }

        return false
    }

    private fun isJwtExpiringSoon(token: String): Boolean {
        val json = decodeJwtPayload(token) ?: return false
        val expiresAt = json.optLong("exp", 0L)
        return expiresAt > 0L &&
            expiresAt <= System.currentTimeMillis() / 1000L + ACCESS_TOKEN_REFRESH_SKEW_SECONDS
    }

    private fun hasPremiumAccessTokenRole(token: String): Boolean {
        val json = decodeJwtPayload(token) ?: return false
        val role = json.optString("role", "").lowercase()
        return PREMIUM_ACCESS_TOKEN_ROLES.contains(role)
    }

    private fun decodeJwtPayload(token: String): JSONObject? {
        val payload = token.split('.').getOrNull(1) ?: return null
        return runCatching {
            val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            JSONObject(String(decoded, Charsets.UTF_8))
        }.getOrNull()
    }

}

private data class ChatBundle(
    val html: String,
    val baseUrl: String,
)

@Composable
private fun ChatBotActivityScreen(
    html: String,
    baseUrl: String,
    onClose: () -> Unit,
) {
    val wvState = rememberWebViewStateWithHTMLData(
        data = html,
        baseUrl = baseUrl
    )

    WebContentLayout(
        title = "ChatFTC",
        onClose = onClose
    ) {
        FtcWebView(
            wvState = wvState,
            initialUrl = baseUrl,
            captureBackPresses = true,
        )
    }
}
