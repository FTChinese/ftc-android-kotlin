package com.ft.ftchinese.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.material.Chip
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.database.SearchEntry
import com.ft.ftchinese.ui.components.IconDelete
import com.ft.ftchinese.ui.components.SubHeading2
import com.ft.ftchinese.ui.theme.Dimens
import com.google.accompanist.flowlayout.FlowRow

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun KeywordHistoryList(
    entries: List<SearchEntry>,
    onClear: () -> Unit,
    onClick: (String) -> Unit
) {

    Column(
        modifier = Modifier.fillMaxWidth()
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
            IconButton(
                onClick = onClear
            ) {
                IconDelete()
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            entries.forEach { entry ->

                Chip(
                    onClick = {
                        onClick(entry.keyword)
                    }
                ) {
                    Text(text = entry.keyword)
                }

                Spacer(modifier = Modifier.width(Dimens.dp4))
            }
        }
    }

}

@Preview(showBackground = true)
@Composable
fun PreviewKeywordHistoryList() {
    KeywordHistoryList(
        entries = listOf(
            SearchEntry(
                keyword = "乌克兰"
            ),
            SearchEntry(
                keyword = "俄罗斯"
            ),
            SearchEntry(
                keyword = "北约"
            ),
            SearchEntry(
                keyword = "拜登",
            ),
            SearchEntry(
                keyword = "约翰逊",
            ),
            SearchEntry(
                keyword = "通胀",
            ),
            SearchEntry(
                keyword = "乌克兰"
            ),
            SearchEntry(
                keyword = "俄罗斯"
            ),
            SearchEntry(
                keyword = "北约"
            ),
            SearchEntry(
                keyword = "拜登",
            ),
            SearchEntry(
                keyword = "约翰逊",
            ),
            SearchEntry(
                keyword = "通胀",
            )
        ),
        onClear = {},
        onClick = {}
    )
}
