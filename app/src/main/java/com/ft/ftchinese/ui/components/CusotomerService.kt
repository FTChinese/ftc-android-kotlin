package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.base.IntentsUtil
import com.ft.ftchinese.ui.theme.OColor
import com.radusalagean.infobarcompose.InfoBar
import com.radusalagean.infobarcompose.InfoBarMessage

@Composable
fun CustomerService() {
    val context = LocalContext.current
    var message: InfoBarMessage? by remember {
        mutableStateOf(null)
    }

    Box {
        Column {
            Text(
                text = stringResource(id = R.string.title_customer_service),
                style = MaterialTheme.typography.h6,
            )
            Divider(color = OColor.teal)
            TextButton(onClick = {
                val intent = IntentsUtil.emailCustomerService("FT中文网会员订阅")
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    message = InfoBarMessage(textStringResId = R.string.prompt_no_email_app)
                }
            }) {
                Text(text = stringResource(id = R.string.customer_service_email))
            }
        }

        InfoBar(offeredMessage = message) {
            message = null
        }
    }

}

@Preview
@Composable
fun PreviewCustomerService() {
    CustomerService()
}
