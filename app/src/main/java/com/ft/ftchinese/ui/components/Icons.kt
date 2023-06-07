package com.ft.ftchinese.ui.components

import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.ft.ftchinese.R

@Composable
fun IconArrowBackIOS(
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_baseline_arrow_back_ios_24),
        contentDescription = "Back",
        tint = tint
    )
}

@Composable
fun IconArrowForwardIOS(
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_baseline_arrow_forward_ios_24),
        contentDescription = "Forward",
        tint = tint
    )
}

@Composable
fun IconArrowBack(
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_baseline_arrow_back_24),
        contentDescription = "Back",
        tint = tint,
    )
}

@Composable
fun IconArrowForward(
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_baseline_arrow_forward_24),
        contentDescription = "Forward",
        tint = tint,
    )
}

@Composable
fun IconRightArrow() {
    Icon(
        painter = painterResource(id = R.drawable.ic_keyboard_arrow_right_gray_24dp),
        contentDescription = "Details"
    )
}

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
fun IconClose(
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_baseline_close_24),
        contentDescription = "Close",
        tint = tint
    )
}

@Composable
fun IconOpenInBrowser(
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_baseline_open_in_browser_24),
        contentDescription = "Open in browser",
        tint = tint,
    )
}

@Composable
fun IconExit(
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_exit_to_app_black_24dp),
        contentDescription = "Exit",
        tint = tint,
    )
}

@Composable
fun IconAccountBox(
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_account_box_black_24dp),
        contentDescription = "Account",
        tint = tint,
    )
}

@Composable
fun IconMember(
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_card_membership_black_24dp),
        contentDescription = "Membership",
        tint = tint,
    )
}

@Composable
fun IconFeedback(
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_feedback_black_24dp),
        contentDescription = "Feedback",
        tint = tint,
    )
}

@Composable
fun IconSettings(
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_settings_black_24dp),
        contentDescription = "Settings",
        tint = tint,
    )
}

@Composable
fun IconInfo(
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_info_black_24dp),
        contentDescription = "Info",
        tint = tint,
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
fun IconRefresh(
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
) {
    Icon(
        painter = painterResource(id = R.drawable.baseline_refresh_black_24),
        contentDescription = "Refresh",
        tint = tint,
    )
}
