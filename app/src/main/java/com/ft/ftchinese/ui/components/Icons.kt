package com.ft.ftchinese.ui.components

import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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
fun IconArrowForward(
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_baseline_arrow_forward_white_24),
        contentDescription = null,
        tint = tint,
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

