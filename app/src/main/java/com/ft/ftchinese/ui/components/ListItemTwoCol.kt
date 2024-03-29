package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun ListItemTwoCol(
    modifier: Modifier = Modifier,
    lead: String,
    tail: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Dimens.dp8)
            .then(modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = lead,
            style = MaterialTheme.typography.subtitle2
        )
        Text(
            text = tail,
            style = MaterialTheme.typography.body1,
            color = OColor.black80
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewListItemTwoCol() {
    ListItemTwoCol(
        lead = "Hello",
        tail = "World"
    )
}
