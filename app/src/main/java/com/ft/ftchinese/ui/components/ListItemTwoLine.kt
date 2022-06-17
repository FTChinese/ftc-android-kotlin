package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun ListItemTwoLine(
    primary: String,
    secondary: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
    ) {
        BodyText1(text = primary)
        Spacer(modifier = Modifier.height(Dimens.dp4))
        BodyText2(text = secondary)
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
