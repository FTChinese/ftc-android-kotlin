package com.ft.ftchinese.ui.settings.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.model.legal.WebpageMeta
import com.ft.ftchinese.model.legal.legalPages
import com.ft.ftchinese.ui.components.ClickableRow
import com.ft.ftchinese.ui.components.IconRightArrow
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun AboutActivityScreen(
    onNavigate: (WebpageMeta) -> Unit
) {
    Column {
        legalPages.forEach { pageMeta ->
            ClickableRow(
                endIcon = {
                    IconRightArrow()
                },
                onClick = {
                  onNavigate(pageMeta)
                },
                contentPadding = PaddingValues(Dimens.dp16)
            ) {
                Text(
                    text = pageMeta.title,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAboutListScreen() {
    AboutActivityScreen(
        onNavigate = {}
    )
}
