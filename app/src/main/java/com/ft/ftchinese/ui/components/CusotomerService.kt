package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.util.IntentsUtil
import com.ft.ftchinese.ui.util.longToast
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun CustomerService() {
    val context = LocalContext.current

    Box {
        Column {
            Text(
                text = stringResource(id = R.string.title_customer_service),
                style = MaterialTheme.typography.h6,
            )
            Divider(color = OColor.teal)
            PlainTextButton(
                onClick = {
                    val ok = IntentsUtil.sendCustomerServiceEmail(context)
                    if (!ok) {
                        context.longToast(R.string.prompt_no_email_app)
                    }
                },
                text = stringResource(id = R.string.customer_service_email)
            )
        }
    }

}

@Preview(showBackground = true)
@Composable
fun PreviewCustomerService() {
    CustomerService()
}
