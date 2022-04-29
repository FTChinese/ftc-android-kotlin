package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun ListItemTwoLine(
    primary: String,
    secondary: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = primary,
            style = MaterialTheme.typography.body1,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = secondary,
            style = MaterialTheme.typography.body2,
            color = OColor.black60,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimens.dp4)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewListItemTwoLine() {
    ListItemTwoLine(
        primary = "Primary",
        secondary = "Secondary",
    )
}
