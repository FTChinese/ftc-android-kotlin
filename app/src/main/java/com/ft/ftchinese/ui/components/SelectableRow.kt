package com.ft.ftchinese.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun SelectableRow(
    modifier: Modifier = Modifier,
    selected: Boolean,
    onSelect: () -> Unit,
    enabled: Boolean = true,
    margin: PaddingValues = PaddingValues(0.dp),
    contentPadding: PaddingValues = PaddingValues(Dimens.dp16),
    background: Color = Color.Transparent,
    startIcon: @Composable RowScope.() -> Unit = {},
    endIcon: @Composable RowScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(margin)
            .selectable(
                enabled = enabled,
                onClick = onSelect,
                selected = selected,
            )
            .background(background)
            .padding(contentPadding)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        startIcon()

        Box(
            modifier = Modifier.weight(1f)
        ) {
            content()
        }

        endIcon()
    }
}
