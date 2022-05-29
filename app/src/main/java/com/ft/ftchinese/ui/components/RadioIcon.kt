package com.ft.ftchinese.ui.components

import androidx.compose.foundation.Image
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun RadioIcon(
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(
            id = if (selected) {
                R.drawable.ic_baseline_check_circle_outline_24
            } else {
                R.drawable.ic_baseline_radio_button_unchecked_24
            }
        ),
        contentDescription = "Select",
        modifier = modifier,
    )
}

@Composable
fun CheckButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(
                id = if (selected) {
                    R.drawable.ic_baseline_check_circle_outline_24
                } else {
                    R.drawable.ic_baseline_radio_button_unchecked_24
                }
            ),
            tint = OColor.teal,
            contentDescription = "Select"
        )
    }
}
