package com.ft.ftchinese.wxapi.shared

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.SecondaryButton
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun WxRespProgress(
    title: String,
    subTitle: String,
    buttonText: String? = stringResource(id = R.string.btn_done),
    onClickButton: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(Dimens.dp16)
            .fillMaxSize()
    ) {
        Text(
            text = title,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.h6
        )

        if (subTitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(Dimens.dp16))
            Text(
                text = subTitle,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.body1
            )
        }

        if (!buttonText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(Dimens.dp16))
            SecondaryButton(
                onClick = onClickButton,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = buttonText)
            }
        }
    }
}
