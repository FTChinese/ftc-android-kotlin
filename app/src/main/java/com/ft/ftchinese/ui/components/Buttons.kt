package com.ft.ftchinese.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R

@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ButtonColors = OButtonDefaults.buttonColors(),
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        content = content,
    )
}

@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = OButtonDefaults.buttonColors(),
        content = {
            Text(text = text)
        },
    )
}

@Composable
fun PrimaryBlockButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    text: String = stringResource(id = R.string.btn_save)
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        colors = OButtonDefaults.buttonColors(),
        content = {
            Text(text = text)
        },
    )
}

@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = OButtonDefaults.outlineButtonColors(),
    content: @Composable() (RowScope.() -> Unit)
) {

    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        border = BorderStroke(
            width = ButtonDefaults.OutlinedBorderSize,
            color = colors.contentColor(enabled = enabled).value,
        ),
        colors = colors,
        content = content,
    )
}

@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    block: Boolean = false,
    text: String,
) {

    val colors = OButtonDefaults.outlineButtonColors()

    OutlinedButton(
        onClick = onClick,
        modifier = if (block) {
            Modifier.fillMaxWidth()
        } else {
            Modifier
        }.then(modifier),
        enabled = enabled,
        border = BorderStroke(
            width = ButtonDefaults.OutlinedBorderSize,
            color = colors.contentColor(enabled = enabled).value,
        ),
        colors = colors,
        content = {
            Text(text = text)
        },
    )
}

@Composable
fun PlainTextButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    colors: ButtonColors = OButtonDefaults.textButtonColors(),
    content: @Composable () -> Unit,
) {
    TextButton(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        colors = colors,
    ) {
        content()
    }
}

@Composable
fun PlainTextButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    text: String,
) {
    TextButton(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        colors = OButtonDefaults.textButtonColors(),
    ) {
        Text(text = text)
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



