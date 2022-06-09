package com.ft.ftchinese.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.theme.OButton
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = OColor.teal,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = OButton.primaryButtonColors(
            backgroundColor = color,
        ),
        content = content,
    )
}

@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = OColor.teal,
    content: @Composable (RowScope.() -> Unit)
) {

    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        border = BorderStroke(
            width = ButtonDefaults.OutlinedBorderSize,
            color = color,
        ),
        colors = OButton.outlinedColors(
            contentColor = color
        ),
        content = content,
    )
}

@Composable
fun OTextButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    colors: ButtonColors = OButton.textColors(),
    text: String,
) {
    TextButton(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        colors = colors,
    ) {
        Text(text = text)
    }
}

enum class ButtonVariant {
    Primary,
    Outline;
}

@Composable
fun BlockButton(
    enabled: Boolean = true,
    onClick: () -> Unit,
    text: String = stringResource(id = R.string.btn_save),
    variant: ButtonVariant = ButtonVariant.Primary,
) {
    val modifier = Modifier.fillMaxWidth()

    when (variant) {
        ButtonVariant.Primary -> {
            PrimaryButton(
                onClick = onClick,
                enabled = enabled,
                modifier = modifier
            ) {
                Text(text = text)
            }
        }
        ButtonVariant.Outline -> {
            OutlinedButton(
                onClick = onClick,
                enabled = enabled,
                modifier = modifier,
            ) {
                Text(text = text)
            }
        }
    }
}

/**
 * IconButton without minimumTouchTargetSize.
 * The default IconButton has a min size limit which
 * is not fit with inline content.
 */
@Composable
fun SlimIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clickable(
                onClick = onClick,
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = rememberRipple()
            ),
        contentAlignment = Alignment.Center
    ) {
        val contentAlpha = if (enabled) LocalContentAlpha.current else ContentAlpha.disabled
        CompositionLocalProvider(LocalContentAlpha provides contentAlpha, content = content)
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPrimaryButton() {
    PrimaryButton(
        onClick = {  }
    ) {
        Text(text = "Primary Button")
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPrimaryButtonDisabled() {
    PrimaryButton(
        onClick = {  },
        enabled = false
    ) {
        Text(text = "Primary Button Disabled")
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSecondaryButton() {
    SecondaryButton(
        onClick = {}
    ) {
       Text(text = "Secondary Button")
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSecondaryButtonDisabled() {
    SecondaryButton(
        onClick = {},
        enabled = false
    ) {
        Text(text = "Secondary Button")
    }
}
