package com.ft.ftchinese.ui.wxinfo

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.Wechat
import com.ft.ftchinese.ui.components.PrimaryButton
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun WxInfoScreen(
    wxInfo: Wechat,
    isLinked: Boolean,
    onLink: () -> Unit,
    onUnlink: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        wxInfo.avatarUrl?.let {
            WxAvatar(
                imageUrl = it,
                name = wxInfo.nickname ?: "",
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Spacer(modifier = Modifier.height(Dimens.dp16))

        // If current wechat is not linked to email,
        // show a button to start email activity.
        if (!isLinked) {
            Text(
                text = stringResource(id = R.string.instruct_link_email),
                style = MaterialTheme.typography.body1,
            )

            PrimaryButton(
                onClick = onLink,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(
                    text = stringResource(id = R.string.btn_link)
                )
            }
        } else {
            // Wechat is linked to email, show a button to
            // unlink email.
            PrimaryButton(
                onClick = onUnlink,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(
                    text = stringResource(id = R.string.btn_unlink)
                )
            }
        }
    }
}

@Composable
fun WxAvatar(
    imageUrl: String,
    name: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
    ) {
        AsyncImage(
            model = imageUrl, 
            contentDescription = "Wechat Avatar"
        )
        
        Text(text = name)
    }
}

/**
 * Used when email is not linked to wechat.
 */
@Composable
fun AlertEmailLinkWx(
    onLinkWx: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "尚未关联微信。绑定微信账号后，可以使用微信账号账号快速登录"
        )
        
        PrimaryButton(
            onClick = { /*TODO*/ }
        ) {
            Text(text = "微信授权")
        }
    }
}
