package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.base.IntentsUtil
import com.ft.ftchinese.ui.theme.OColor
import kotlinx.coroutines.launch

@Composable
fun CustomerService(
    onError: (String) -> Unit,
) {
    val context = LocalContext.current

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
                onError(context.getString(R.string.prompt_no_email_app))
            }
        }) {
            Text(text = stringResource(id = R.string.customer_service_email))
        }
    }
}

@Preview
@Composable
fun PreviewCustomerService() {
    CustomerService(
        onError = { }
    )
}
