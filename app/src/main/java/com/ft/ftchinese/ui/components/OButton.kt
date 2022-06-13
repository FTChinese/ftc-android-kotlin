package com.ft.ftchinese.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor

object OButtonDefaults {

    val ContentPadding = PaddingValues(
        horizontal = Dimens.dp8,
        vertical = Dimens.dp4,
    )

    @Composable
    fun buttonColors(
        backgroundColor: Color = OColor.teal,
        contentColor: Color = OColor.white,
    ): ButtonColors = ButtonDefaults.buttonColors(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        disabledBackgroundColor = backgroundColor.copy(alpha = ContentAlpha.disabled)
    )

    @Composable
    fun outlineButtonColors(
        backgroundColor: Color = Color.Transparent,
        contentColor: Color = OColor.teal,
    ): ButtonColors = ButtonDefaults.outlinedButtonColors(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        disabledContentColor = contentColor.copy(alpha = ContentAlpha.disabled)
    )

    @Composable
    fun outlineButtonDanger(
        backgroundColor: Color = Color.Transparent,
        contentColor: Color = OColor.claret,
    ): ButtonColors = ButtonDefaults.outlinedButtonColors(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        disabledContentColor = contentColor.copy(alpha = ContentAlpha.disabled)
    )

    @Composable
    fun textButtonColors(
        backgroundColor: Color = Color.Transparent,
        contentColor: Color = OColor.teal,
    ): ButtonColors = ButtonDefaults.textButtonColors(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        disabledContentColor = contentColor.copy(alpha = 0.4F),
    )
}

@Composable
fun SelectButton(
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    border: BorderStroke? = null,
    colors: ButtonColors = OButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = OButtonDefaults.ContentPadding,
    content: @Composable () -> Unit,
) {

    val contentColor by colors.contentColor(enabled)

    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalContentAlpha provides contentColor.alpha
    ) {
        ProvideTextStyle(
            value = MaterialTheme.typography.button
        ) {
            Row(
                modifier = modifier
                    .selectable(
                        selected = selected,
                        enabled = enabled,
                        onClick = onSelect
                    )
                    .then(if (border != null) Modifier.border(border, shape) else Modifier)
                    .clip(shape = shape)
                    .background(colors.backgroundColor(enabled).value)
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                content()
            }
        }
    }
}

@Composable
fun OButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    border: BorderStroke? = null,
    colors: ButtonColors = OButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = OButtonDefaults.ContentPadding,
    content: @Composable () -> Unit,
) {

    val contentColor by colors.contentColor(enabled)

    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalContentAlpha provides contentColor.alpha
    ) {
        ProvideTextStyle(
            value = MaterialTheme.typography.button
        ) {
            Row(
                modifier = modifier
                    .clickable(
                        enabled = enabled,
                        onClick = onClick
                    )
                    .then(if (border != null) Modifier.border(border, shape) else Modifier)
                    .clip(shape = shape)
                    .background(colors.backgroundColor(enabled).value)
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                content()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSelectButton() {
    SelectButton(
        selected = true,
        onSelect = {  },
        modifier = Modifier
            .padding(Dimens.dp8)
    ) {
        SubHeading2(text = "English")
    }
}