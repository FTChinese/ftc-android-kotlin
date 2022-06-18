package com.ft.ftchinese.ui.form

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.ft.ftchinese.model.content.legalPages
import com.ft.ftchinese.ui.theme.OColor
import com.ft.ftchinese.ui.webpage.WebpageActivity

@Composable
fun AutoRenewAgreement() {
    val context = LocalContext.current
    val page = legalPages[2]

    val annotatedString = buildAnnotatedString {
        append("点击订阅即同意FT中文网")

        pushStringAnnotation(
            "autoRenew",
            annotation = page.url
        )
        withStyle(
            style = SpanStyle(
                color = OColor.claret
            )
        ) {
            append("《${page.title}》")
        }
        pop()

        append("相关内容")
    }

    ClickableText(
        text = annotatedString,
        onClick = { offset ->
            annotatedString.getStringAnnotations(
                tag = "autoRenew",
                start = offset,
                end = offset
            ).firstOrNull()?.let {
                val url = it.item
                WebpageActivity.start(
                    context = context,
                    meta = page
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}
