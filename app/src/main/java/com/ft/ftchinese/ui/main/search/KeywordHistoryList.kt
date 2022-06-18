package com.ft.ftchinese.ui.main.search

import androidx.compose.foundation.layout.*
import androidx.compose.material.Chip
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.ui.components.IconDelete
import com.ft.ftchinese.ui.components.SlimIconButton
import com.ft.ftchinese.ui.components.SubHeading2
import com.ft.ftchinese.ui.theme.Dimens
import com.google.accompanist.flowlayout.FlowRow

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun KeywordHistoryList(
    modifier: Modifier = Modifier,
    keywords: List<String>,
    onClear: () -> Unit,
    onClick: (String) -> Unit
) {

    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            SubHeading2(
                text = "搜索历史",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start
            )

            SlimIconButton(onClick = onClear) {
                IconDelete()
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            keywords.forEach { kw ->

                Chip(
                    onClick = {
                        onClick(kw)
                    }
                ) {
                    Text(text = kw)
                }

                Spacer(modifier = Modifier.width(Dimens.dp8))
            }
        }
    }

}

@Preview(showBackground = true)
@Composable
fun PreviewKeywordHistoryList() {
    KeywordHistoryList(
        keywords = listOf(
            "caravan",
            "magna cum laude",
            "samaritan",
            "aneurysm",
            "gird",
            "aphasia",
            "saber-rattling",
            "conviviality",
            "walt-out",
            "shrew",
            "vendetta"
        ),
        onClear = {},
        onClick = {}
    )
}
