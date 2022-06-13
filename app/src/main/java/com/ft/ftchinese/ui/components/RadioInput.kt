package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun RadioInput(
    modifier: Modifier = Modifier,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    label: String,
) {
    Row(
        modifier = modifier
            .selectable(
                selected = selected,
                enabled = enabled,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(
                selectedColor = OColor.teal,
            )
        )

        Text(
            text = label,
            color = LocalContentColor.current.copy(
                if (enabled) {
                    LocalContentAlpha.current
                } else {
                    ContentAlpha.disabled
                }
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewRadioInput() {
    RadioInput(
        selected = true,
        enabled = false,
        onClick = {  },
        label = "Radio 1"
    )
}
