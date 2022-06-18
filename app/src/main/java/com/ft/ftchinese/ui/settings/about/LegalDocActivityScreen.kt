package com.ft.ftchinese.ui.settings.about

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.model.legal.legalPages
import com.ft.ftchinese.ui.util.toast
import com.ft.ftchinese.ui.webpage.WebpageScreen

@Composable
fun LegalDocActivityScreen(
    index: Int,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val page = remember(index) {
        legalPages.getOrNull(index = index)
    }

    if (page == null) {
        context.toast("Data not found!")
        return
    }

    WebpageScreen(
        pageMeta = page,
        onClose = onClose,
    )
}
