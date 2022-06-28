package com.ft.ftchinese.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
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
    fun outlineBorder(
        color: Color = OColor.teal
    ): BorderStroke = BorderStroke(
        width = ButtonDefaults.OutlinedBorderSize,
        color = color
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

    @Composable
    fun selectButtonColors(
        backgroundColor: Color = OColor.teal,
        contentColor: Color = OColor.white,
        unselectedBackgroundColor: Color = Color.Transparent,
        unselectedContentColor: Color = OColor.teal,
    ): ButtonColors = ButtonDefaults.buttonColors(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        disabledBackgroundColor = unselectedBackgroundColor,
        disabledContentColor = unselectedContentColor
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
    colors: ButtonColors = OButtonDefaults.selectButtonColors(),
    contentPadding: PaddingValues = OButtonDefaults.ContentPadding,
    content: @Composable () -> Unit,
) {

    val contentColor by colors.contentColor(selected)

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
                    .background(colors.backgroundColor(selected).value)
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

@Composable
fun OButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    border: BorderStroke? = null,
    colors: ButtonColors = OButtonDefaults.buttonColors(),
    margin: PaddingValues = PaddingValues(0.dp),
    contentPadding: PaddingValues = OButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
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
                modifier = Modifier
                    .padding(margin)
                    .clickable(
                        enabled = enabled,
                        onClick = onClick
                    )
                    .then(if (border != null) Modifier.border(border, shape) else Modifier)
                    .clip(shape = shape)
                    .background(colors.backgroundColor(enabled).value)
                    .padding(contentPadding)
                    .then(modifier),
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
fun PreviewOButton() {
    OButton(
        onClick = {  },
        colors = OButtonDefaults.outlineButtonColors(),
        border = BorderStroke(1.dp, OColor.teal),
        shape = CircleShape,
        margin = PaddingValues(Dimens.dp16),
        modifier = Modifier.size(50.dp)
    ) {
        Text(text = "Hello, Word")
    }
}


