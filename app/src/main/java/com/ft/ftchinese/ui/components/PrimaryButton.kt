package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(0),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = OColor.teal,
            contentColor = OColor.white,
            disabledBackgroundColor = OColor.teal.copy(alpha = 0.4f)
        ),
        content = content,
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewPrimaryButton() {
    PrimaryButton(
        onClick = {  }
    ) {
        Text(text = "Button")
    }
}
