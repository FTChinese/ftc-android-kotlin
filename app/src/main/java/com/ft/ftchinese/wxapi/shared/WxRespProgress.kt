package com.ft.ftchinese.wxapi.shared

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.ui.components.SecondaryButton
import com.ft.ftchinese.ui.subs.member.SubsStatus
import com.ft.ftchinese.ui.subs.member.SubsStatusCard
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun WxRespProgress(
    title: String,
    subTitle: String,
    buttonText: String? = stringResource(id = R.string.btn_done),
    onClickButton: () -> Unit,
    subscribed: Membership? = null
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

        if (subscribed != null) {
            Spacer(modifier = Modifier.height(Dimens.dp16))
            SubsStatusCard(
                status = SubsStatus.newInstance(
                    ctx = LocalContext.current,
                    m = subscribed
                )
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
