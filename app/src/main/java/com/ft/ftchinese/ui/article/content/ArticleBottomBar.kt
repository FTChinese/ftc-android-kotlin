package com.ft.ftchinese.ui.article.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.SlimIconButton
import com.ft.ftchinese.ui.components.SlimIconToggleButton
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun ArticleBottomBar(
    bookmarked: Boolean,
    onBookmark: (Boolean) -> Unit,
    onShare: () -> Unit,
    backgroundColor: Color = MaterialTheme.colors.primarySurface,
) {
    Row(
        modifier = Modifier
            .background(backgroundColor)
            .padding(
                horizontal = Dimens.dp16,
                vertical = Dimens.dp8,
            )
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SlimIconButton(
            onClick = onShare
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_share_black_24dp),
                contentDescription = "Share",
                tint = OColor.teal
            )
        }

        SlimIconToggleButton(
            checked = bookmarked,
            onCheckedChange = onBookmark,
        ) {
            Icon(
                painter = painterResource(id = if (bookmarked) {
                    R.drawable.ic_bookmark_black_24dp
                } else {
                    R.drawable.ic_bookmark_border_black_24dp
                }),
                contentDescription = "Bookmark",
                tint = OColor.teal
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewBottomTool() {
    ArticleBottomBar(
        bookmarked = true,
        onBookmark = {},
        onShare = {}
    )
}
