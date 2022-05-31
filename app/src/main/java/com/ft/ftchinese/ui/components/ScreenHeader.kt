package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun ScreenHeader(
    title: String,
    subTitle: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (title.isNotBlank()) {
            Heading2(text = title)
            Spacer(modifier = Modifier.height(Dimens.dp4))
        }

        if (subTitle.isNotBlank()) {
            SubHeading2(text = subTitle)
            Spacer(modifier = Modifier.height(Dimens.dp8))
        }
    }
}