package com.ft.ftchinese.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun ButtonGroupLayout(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .selectableGroup()
            .border(1.dp, OColor.teal, MaterialTheme.shapes.small)
            .padding(2.dp)
            .then(modifier)
    ) {
        content()
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewButtonGroup() {
    ButtonGroupLayout {
        SelectButton(
            onSelect = {  },
            selected = true
        ) {
            SubHeading2(text = "Chinese")
        }

        SelectButton(
            selected = false,
            onSelect = {  },
            colors = OButtonDefaults.textButtonColors()
        ) {
            SubHeading2(text = "English")
        }

        SelectButton(
            selected = false,
            onSelect = {  },
            colors = OButtonDefaults.textButtonColors()
        ) {
            SubHeading2(text = "Bilingual")
        }
    }
}
