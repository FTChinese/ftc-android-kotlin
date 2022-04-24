package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun ListItemTwoCol(
    lead: String,
    tail: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Dimens.dp8),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = lead,
            style = MaterialTheme.typography.body1
        )
        Text(
            text = tail,
            style = MaterialTheme.typography.body1,
            color = OColor.black80
        )
    }
}
