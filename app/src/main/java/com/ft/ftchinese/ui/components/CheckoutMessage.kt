package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun CheckoutMessage(
    text: String,
) {
    if (text.isNotBlank()) {
        Text(
            text = text,
            style = MaterialTheme.typography.body2,
            color = OColor.claret,
            modifier = Modifier.fillMaxWidth()
                .padding(Dimens.dp8),
            textAlign = TextAlign.Center
        )
    }
}
