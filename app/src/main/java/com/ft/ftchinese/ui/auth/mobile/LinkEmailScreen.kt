package com.ft.ftchinese.ui.auth.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.ft.ftchinese.ui.components.TipText
import com.ft.ftchinese.ui.components.rememberInputState
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.validator.passwordRules
import com.ft.ftchinese.ui.validator.ruleEmailValid

@Composable
fun LinkEmailScreen(
    mobile: String,
) {

    val emailState = rememberInputState(
        rules = listOf(
            ruleEmailValid,
        )
    )

    val pwState = rememberInputState(
        rules = passwordRules()
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.dp16)
    ) {
        Text(
            text = "关联已有邮箱账号",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.h5,
            textAlign = TextAlign.Center
        )
        TipText(
            text = "绑定邮箱后下次可以直接使用手机号${mobile}登录该邮箱账号"
        )

        Spacer(modifier = Modifier.height(Dimens.dp16))


    }
}
