package com.ft.ftchinese.ui.settings.fontsize

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.model.enums.FontSize

@Composable
fun FontSizeScreen(
    selected: FontSize,
    onSelect: (FontSize) -> Unit,
) {

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
        ) {
            listOf(
                FontSize.Smallest,
                FontSize.Smaller,
                FontSize.Normal,
                FontSize.Bigger,
                FontSize.Biggest,
            )
                .forEach { size ->
                    FontSizeItem(
                        fs = size,
                        selected = (selected == size),
                        onSelect = onSelect
                    )
                }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewFontSizeActivityScreen() {
    FontSizeScreen(
        selected = FontSize.Normal,
        onSelect = {}
    )
}
