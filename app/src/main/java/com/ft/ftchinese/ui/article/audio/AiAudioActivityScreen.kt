package com.ft.ftchinese.ui.article.audio

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.ui.article.NavStore
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.util.UriUtils
import com.ft.ftchinese.ui.util.toast
import com.ft.ftchinese.ui.web.FtcWebView
import com.ft.ftchinese.viewmodel.UserViewModel
import com.google.accompanist.web.rememberWebViewState
import com.google.accompanist.web.rememberWebViewStateWithHTMLData

private const val TAG = "AiAudioScreen"
private const val LOG_PREFIX = "[FTCPush]"

@Composable
fun AiAudioActivityScreen(
    userViewModel: UserViewModel = viewModel(),
    id: String?
) {
    val context = LocalContext.current
    val accountState = userViewModel.accountLiveData.observeAsState()

    if (id == null) {
        context.toast("Missing id")
        return
    }

    val teaser = remember(id) {
        NavStore.getTeaser(id)
    }

    if (teaser == null) {
        context.toast("Missing teaser")
        return
    }

    val directAudioUrl = remember(teaser) {
        teaser.audioUrl?.takeIf { it.isNotBlank() }
            ?: teaser.radioUrl?.takeIf { it.isNotBlank() }
    }

    val audioPageUrl = remember(teaser, accountState.value) {
        UriUtils.teaserAudioPageUrl(teaser, accountState.value)
    }

    if (directAudioUrl.isNullOrBlank() && audioPageUrl.isNullOrBlank()) {
        context.toast("Missing required url")
        return
    }

    val title = teaser.title.ifBlank { "FT Audio" }

    val webViewState = if (!audioPageUrl.isNullOrBlank()) {
        Log.i(
            TAG,
            "$LOG_PREFIX audio_screen mode=page_audio title=$title url=$audioPageUrl"
        )
        rememberWebViewState(url = audioPageUrl)
    } else {
        Log.i(
            TAG,
            "$LOG_PREFIX audio_screen mode=direct_audio_fallback title=$title url=${directAudioUrl?.take(48)}"
        )
        rememberWebViewStateWithHTMLData(
            data = buildInlineAudioHtml(
                title = title,
                audioUrl = directAudioUrl!!,
            ),
            baseUrl = "https://www.ftchinese.com"
        )
    }

    ProgressLayout(
        loading = webViewState.isLoading,
        modifier = Modifier.fillMaxSize()
    ) {
        FtcWebView(
            wvState = webViewState,
        )
    }

}

private fun buildInlineAudioHtml(
    title: String,
    audioUrl: String,
): String {
    val escapedTitle = android.text.TextUtils.htmlEncode(title)
    val escapedAudioUrl = android.text.TextUtils.htmlEncode(audioUrl)

    return """
        <!DOCTYPE html>
        <html lang="zh-CN">
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0" />
          <title>$escapedTitle</title>
          <style>
            body {
              margin: 0;
              padding: 24px;
              background: #fff1e5;
              color: #33302e;
              font-family: sans-serif;
            }
            .card {
              max-width: 720px;
              margin: 0 auto;
              background: #ffffff;
              border-radius: 16px;
              padding: 24px;
              box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
            }
            h1 {
              margin: 0 0 16px;
              font-size: 24px;
              line-height: 1.4;
            }
            audio {
              width: 100%;
              margin-top: 12px;
            }
            .meta {
              margin-top: 12px;
              font-size: 14px;
              color: #6b625d;
              word-break: break-all;
            }
          </style>
        </head>
        <body>
          <div class="card">
            <h1>$escapedTitle</h1>
            <audio controls autoplay playsinline preload="metadata" src="$escapedAudioUrl"></audio>
            <div class="meta">$escapedAudioUrl</div>
          </div>
        </body>
        </html>
    """.trimIndent()
}
