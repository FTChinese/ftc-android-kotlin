package com.ft.ftchinese.ui.webpage

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.ui.components.CloseBar
import com.ft.ftchinese.ui.components.MenuOpenInBrowser
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.web.UrlHandler

@Composable
fun WebContentLayout(
    modifier: Modifier = Modifier,
    url: String? = null,
    title: String = "",
    loading: Boolean = false,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier)
    ) {
        CloseBar(
            onClose = onClose,
            title = title,
            actions = {
                if (url != null) {
                    MenuOpenInBrowser {
                        UrlHandler.openInCustomTabs(
                            ctx = context,
                            url = Uri.parse(url)
                        )
                    }
                }
            }
        )

        ProgressLayout(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            loading = loading
        ) {
            content()
        }

    }
}
