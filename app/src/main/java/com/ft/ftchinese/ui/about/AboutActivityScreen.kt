package com.ft.ftchinese.ui.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.model.legal.WebpageMeta
import com.ft.ftchinese.model.legal.legalPages
import com.ft.ftchinese.ui.components.ClickableRow
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun AboutActivityScreen(
    onNavigate: (WebpageMeta) -> Unit
) {
    Column {
        legalPages.forEach { pageMeta ->
            ClickableRow(
                onClick = {
                  onNavigate(pageMeta)
                },
                modifier = Modifier
                    .padding(Dimens.dp16)
            ) {
                Text(
                    text = pageMeta.title,
                    modifier = Modifier.weight(1f)
                )
            }

            Divider(startIndent = Dimens.dp16)
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