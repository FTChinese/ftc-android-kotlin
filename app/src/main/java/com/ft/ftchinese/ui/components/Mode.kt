package com.ft.ftchinese.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.model.enums.ApiMode
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun Mode(
    mode: ApiMode
) {
    Text(
        text = mode.name,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(OColor.claret)
            .padding(Dimens.dp8),
        style = MaterialTheme.typography.h6,
        color = OColor.white
    )
    Spacer(modifier = Modifier.height(Dimens.dp8))
}

@Preview(showBackground = true)
@Composable
fun PreviewMode() {
    Mode(mode = ApiMode.Sandbox)
}
