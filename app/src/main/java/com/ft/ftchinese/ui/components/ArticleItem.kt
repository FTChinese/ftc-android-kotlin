package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.em
import com.ft.ftchinese.ui.theme.Dimens

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ArticleItem(
    heading: String,
    subHeading: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(Dimens.dp16)
        ) {
            Text(
                text = heading,
                modifier = Modifier
                    .fillMaxWidth(),
                style = MaterialTheme
                    .typography
                    .h6
            )

            Text(
                text = subHeading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.dp16),
                style = MaterialTheme
                    .typography
                    .body2,
                lineHeight = 2.em
            )
        }
    }
}

@Preview
@Composable
fun PreviewArticleItem() {
    ArticleItem(
        heading = "美国高官：有进一步制裁俄罗斯的长篇剧本",
        subHeading = "美国国务院副国务卿何塞·费尔南德表示，美国计划对俄罗斯试试进一步制裁的剧本很长，而且几乎看不到取消下游制裁的余地"
    ) {

    }
}
