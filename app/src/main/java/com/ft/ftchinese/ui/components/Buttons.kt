package com.ft.ftchinese.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = OColor.teal,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = OColor.white,
            disabledBackgroundColor = backgroundColor.copy(alpha = 0.4f)
        ),
        content = content,
    )
}

@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = OColor.paper,
    contentColor: Color = OColor.teal,
    content: @Composable RowScope.() -> Unit
) {
    LocalContentColor
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        border = BorderStroke(1.dp, contentColor),
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            disabledContentColor = contentColor.copy(alpha = 0.4f)
        ),
        content = content,
    )
}

enum class ButtonVariant {
    Primary,
    Outline;
}

@Composable
fun BlockButton(
    enabled: Boolean,
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
