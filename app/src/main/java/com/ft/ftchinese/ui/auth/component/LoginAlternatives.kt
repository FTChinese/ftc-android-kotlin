package com.ft.ftchinese.ui.auth.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ft.ftchinese.R

@Composable
fun LoginAlternatives(
    onClickEmail: () -> Unit,
    onClickWechat: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceAround,
        modifier = Modifier.fillMaxWidth()
    ) {

        IconButton(
            onClick = onClickEmail
        ) {
            Image(
                painter = painterResource(id = R.drawable.icons8_circled_envelope_100),
                contentDescription = stringResource(id = R.string.title_email_login),
                modifier = Modifier.height(48.dp),
                contentScale = ContentScale.Fit
            )
        }

        IconButton(
            onClick = onClickWechat
        ) {
            Image(
                painter = painterResource(id = R.drawable.wechat_round_100),
                contentDescription = stringResource(id = R.string.title_wx_login),
                modifier = Modifier
                    .height(48.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLoginAlternatives() {
    LoginAlternatives(
        onClickEmail = { }
    ) {

    }
}
