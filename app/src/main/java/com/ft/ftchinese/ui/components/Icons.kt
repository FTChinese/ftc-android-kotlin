package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ft.ftchinese.R

@Composable
fun IconAddCircle(
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_baseline_add_circle_outline_24),
        contentDescription = "Add",
        tint = tint
    )
}

@Composable
fun RightArrow() {
    Icon(
        painter = painterResource(id = R.drawable.ic_keyboard_arrow_right_gray_24dp),
        contentDescription = "Details"
    )
}

@Composable
fun IconCancel(
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_baseline_cancel_24),
        contentDescription = "Cancel",
        tint = tint
    )
}

@Composable
fun IconRedo(
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_baseline_redo_24),
        contentDescription = "Redo",
        tint = tint
    )
}

@Composable
fun IconDelete(
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_delete_forever_black_24dp),
        contentDescription = "Delete",
        tint = tint
    )
}

@Composable
fun IconFolderOpen(
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_baseline_folder_open_24),
        contentDescription = "Folder",
        tint = tint,
    )
}

@Composable
fun IconSearch(
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_search_black_24dp),
        contentDescription = "Search",
        tint = tint,
    )
}

@Composable
fun IconCheck(
    checked: Boolean,
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
) {
    Box(
        modifier = Modifier
            .width(24.dp)
            .height(24.dp)
    ) {

        Icon(
            painter = painterResource(id = if (checked) {
                R.drawable.ic_baseline_check_circle_outline_24
            } else {
                R.drawable.ic_baseline_radio_button_unchecked_24
            }),
            contentDescription = "Checked",
            modifier = Modifier.align(Alignment.Center),
            tint = tint
        )
    }
}
