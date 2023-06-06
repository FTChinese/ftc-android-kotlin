package com.ft.ftchinese.ui.settings.fontsize

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.ft.ftchinese.model.enums.FontSize
import com.ft.ftchinese.ui.components.CheckVariant
import com.ft.ftchinese.ui.components.OCheckbox
import com.ft.ftchinese.ui.components.SelectableRow

@Composable
fun FontSizeItem(
    fs: FontSize,
    selected: Boolean,
    onSelect: (FontSize) -> Unit
) {
    SelectableRow(
        selected = selected,
        onSelect = { onSelect(fs) },
        endIcon = {
            OCheckbox(
                checked = selected,
                onCheckedChange = { onSelect(fs) },
                variant = CheckVariant.Square,
            )
        }
    ) {
        Text(
            text = stringResource(id = fs.stringId),
            style = TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = fs.size.sp,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewFontSizeItem() {
    FontSizeItem(
        fs = FontSize.Normal,
        selected = true,
        onSelect = {}
    )
}
