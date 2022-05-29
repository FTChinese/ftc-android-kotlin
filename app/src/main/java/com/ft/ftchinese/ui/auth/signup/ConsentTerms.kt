package com.ft.ftchinese.ui.auth.signup

import androidx.compose.foundation.layout.Row
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.model.legal.legalPages
import com.ft.ftchinese.ui.theme.OButton
import com.ft.ftchinese.ui.webpage.WebpageActivity

@Composable
fun ConsentTerms(
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val context = LocalContext.current
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect
        )
        Text(text = "我已阅读并同意")
        TextButton(
            onClick = {
                WebpageActivity.start(
                    context,
                    legalPages[0]
                )
            },
            colors = OButton.textColors()
        ) {
            Text(text = "《用户协议》")
        }
        Text(text = "和")
        TextButton(
            onClick = {
                WebpageActivity.start(
                    context,
                    legalPages[1]
                )
            },
            colors = OButton.textColors()
        ) {
            Text(text = "《隐私政策》")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewConsentTerms() {
    ConsentTerms(
        selected = true,
        onSelect = { },
    )
}
