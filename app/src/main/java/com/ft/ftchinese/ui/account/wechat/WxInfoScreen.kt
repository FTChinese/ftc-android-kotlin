package com.ft.ftchinese.ui.account.wechat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.Wechat
import com.ft.ftchinese.ui.components.PrimaryButton
import com.ft.ftchinese.ui.theme.Dimens

/**
 * Used in two cases:
 * 1. Wechat-only logg-in;
 * 2. Email login with wechat linked.
 * In both cases Account#unionId field is not empty.
 */
@Composable
fun WxInfoScreen(
    wechat: Wechat,
    isLinked: Boolean,
    onLinkEmail: () -> Unit, // For wechat-only user to link an email account
    onUnlinkEmail: () -> Unit, // For linked account to unlink.
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.dp16)
            .verticalScroll(
                rememberScrollState()
            )
    ) {

        NamedAvatar(
            imageUrl = wechat.avatarUrl,
            name = wechat.nickname ?: "",
        )

        Spacer(modifier = Modifier.height(Dimens.dp16))

        // If current wechat is not linked to email,
        // show a button so that user could launch a new screen
        // to verify or create an email account.
        if (!isLinked) {
            Text(
                text = stringResource(id = R.string.instruct_link_email),
                style = MaterialTheme.typography.body1,
            )

            Spacer(modifier = Modifier.height(Dimens.dp16))

            PrimaryButton(
                onClick = onLinkEmail,
                modifier = Modifier.align(Alignment.End),
                text = stringResource(id = R.string.btn_link)
            )
        } else {

            // Wechat is linked to email, show a button to
            // unlink email.
            PrimaryButton(
                onClick = onUnlinkEmail,
                modifier = Modifier.align(Alignment.End),
                text = stringResource(id = R.string.btn_unlink)
            )
        }
    }
}

@Composable
private fun NamedAvatar(
    imageUrl: String?,
    name: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        AsyncImage(
            model = imageUrl, 
            contentDescription = "",
            placeholder = painterResource(id = R.drawable.ic_account_circle_black_24dp),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .height(128.dp)
                .width(128.dp)
                .clip(RoundedCornerShape(10.dp))
        )
        Text(
            text = name,
            style = MaterialTheme.typography.subtitle2,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewWxInfoScreen() {
    WxInfoScreen(
        wechat = Wechat(
            avatarUrl = null,
            nickname = "Wechat User"
        ),
        isLinked = false,
        onLinkEmail =
        {  }
    ) {

    }
}
