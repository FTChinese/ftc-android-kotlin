package com.ft.ftchinese.ui.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.model.legal.WebpageMeta
import com.ft.ftchinese.model.legal.legalPages
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun AboutActivityScreen(
    onNavigate: (WebpageMeta) -> Unit
) {
    Column {
        legalPages.forEach { pageMeta ->
            Row(
                modifier = Modifier
                    .clickable {
                        onNavigate(pageMeta)
                    }
                    .fillMaxWidth()
                    .padding(Dimens.dp16),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = pageMeta.title)
                Icon(
                    painter = painterResource(id = R.drawable.ic_keyboard_arrow_right_gray_24dp),
                    contentDescription = "Open"
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
