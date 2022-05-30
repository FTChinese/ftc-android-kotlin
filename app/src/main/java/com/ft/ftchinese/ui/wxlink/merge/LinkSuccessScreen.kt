package com.ft.ftchinese.ui.wxlink.merge

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.BlockButton
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun LinkSuccessScreen(
    onFinish: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(Dimens.dp16)
            .fillMaxWidth()
    ) {
        Text(
            text = "账号已绑定！",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Dimens.dp16))

        BlockButton(
            enabled = true,
            onClick = onFinish,
            text = stringResource(id = R.string.btn_ok)
        )
    }
}
