package com.ft.ftchinese.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.theme.OColor

enum class CheckVariant(
    @DrawableRes val selectedIcon: Int,
    @DrawableRes val unselectedIcon: Int,
) {
    Square(
        selectedIcon = R.drawable.ic_baseline_check_box_24,
        unselectedIcon = R.drawable.ic_baseline_check_box_outline_blank_24
    ),
    Circle(
        selectedIcon = R.drawable.ic_baseline_check_circle_24,
        unselectedIcon = R.drawable.ic_baseline_radio_button_unchecked_24
    ),
    Radio(
        selectedIcon = R.drawable.ic_baseline_radio_button_checked_24,
        unselectedIcon = R.drawable.ic_baseline_radio_button_unchecked_24
    )
}

@Composable
fun OCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = OColor.teal,
    variant: CheckVariant = CheckVariant.Square,
) {
    SlimIconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
    ) {
        Icon(
            painter = painterResource(
                id = if (checked) {
                    variant.selectedIcon
                } else {
                    variant.unselectedIcon
                }
            ),
            contentDescription = null,
            tint = if (enabled) {
                color
            } else {
                LocalContentColor.current.copy(
                    ContentAlpha.disabled
                )
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewOCheckbox_Square() {
    OCheckbox(
        checked = true,
        onCheckedChange = {  }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewOCheckbox_Circle() {
    OCheckbox(
        checked = true,
        onCheckedChange = { },
        variant = CheckVariant.Circle
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewOCheckbox_Radio() {
    OCheckbox(
        checked = true,
        onCheckedChange = {},
        variant = CheckVariant.Radio
    )
}
