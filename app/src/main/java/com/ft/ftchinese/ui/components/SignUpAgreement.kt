package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.model.legal.legalPages
import com.ft.ftchinese.ui.theme.OColor
import com.ft.ftchinese.ui.webpage.WebpageActivity

@Composable
fun ConsentTerms(
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val context = LocalContext.current

    val annotatedString = buildAnnotatedString {
        append("我已阅读并同意")
        pushStringAnnotation(
            "service",
            legalPages[0].url
        )
        withStyle(
            style = SpanStyle(
                color = OColor.claret
            )
        ) {
            append("《${legalPages[0].title}》")
        }
        pop()
        append("和")
        pushStringAnnotation(
            "privacy",
            legalPages[1].url
        )
        withStyle(
            style = SpanStyle(
                color = OColor.claret
            )
        ) {
            append("《${legalPages[1].title}》")
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        CheckButton(
            selected = selected,
            onClick = onSelect
        )

        ClickableText(
            text = annotatedString,
            onClick = { offset ->
                annotatedString.getStringAnnotations(
                    tag = "service",
                    start = offset,
                    end = offset
                ).firstOrNull()?.let {
                    WebpageActivity.start(
                        context = context,
                        meta = legalPages[0]
                    )
                }

                annotatedString.getStringAnnotations(
                    tag = "privacy",
                    start = offset,
                    end = offset
                ).firstOrNull()?.let {
                    WebpageActivity.start(
                        context = context,
                        meta = legalPages[1]
                    )
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewConsentTerms() {
    ConsentTerms(
        selected = true,
        onSelect = { },
    )
}
