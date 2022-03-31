package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.ui.formatter.FormatHelper
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun CheckoutHeader(
    tier: Tier,
) {
    Text(
        text = FormatHelper.getTier(LocalContext.current, tier),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Dimens.dp8),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.h5,
    )
}
