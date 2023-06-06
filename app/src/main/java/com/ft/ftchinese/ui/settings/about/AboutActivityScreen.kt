package com.ft.ftchinese.ui.settings.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.model.content.legalPages
import com.ft.ftchinese.ui.components.ClickableRow
import com.ft.ftchinese.ui.components.IconRightArrow
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun AboutActivityScreen(
    onNavigate: (index: Int) -> Unit
) {
    Column {
        legalPages.forEachIndexed { index, webpageMeta ->
            ClickableRow(
                endIcon = {
                    IconRightArrow()
                },
                onClick = {
                  onNavigate(index)
                },
                contentPadding = PaddingValues(Dimens.dp16)
            ) {
                Text(
                    text = webpageMeta.title,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAboutScreen() {
    AboutActivityScreen(
        onNavigate = {}
    )
}
