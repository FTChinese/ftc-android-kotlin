package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.ft.ftchinese.R
import com.ft.ftchinese.model.request.MobileFormParams
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.validator.ValidationRule
import com.ft.ftchinese.ui.validator.Validator

@Composable
fun MobileForm(
    defaultMobile: String,
    loading: Boolean,
    timerState: TimerState,
    onRequestCode: (String) -> Unit, // Pass user entered mobile to host.
    onSave: (MobileFormParams) -> Unit, // Pass mobile number and verification code back.
) {

    val mobileState = rememberInputState(
        initialValue = defaultMobile,
        rules = listOf(
            ValidationRule(
                predicate = Validator::isMainlandPhone,
                message = "请输入正确的手机号码"
            ),
            ValidationRule(
                predicate = {
                    it != defaultMobile
                },
                message = "手机已设置"
            )
        )
    )

    val codeState = rememberInputState(
        rules = listOf(
            ValidationRule(
                predicate = Validator.minLength(6),
                message = "请输入验证码"
            )
        )
    )

    val formValid = mobileState.valid.value && codeState.valid.value && !loading

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        TextInput(
            label = stringResource(id = R.string.mobile_phone),
            state = mobileState,
            keyboardType = KeyboardType.Number,
        )
        TipText(text = stringResource(id = R.string.mobile_mainland_only))

        Spacer(modifier = Modifier.height(Dimens.dp8))

        TextInput(
            label = stringResource(id = R.string.mobile_verification_code),
            state = codeState,
            keyboardType = KeyboardType.Number,
            trailingIcon = {
                TextButton(
                    onClick = {
                        onRequestCode(mobileState.field.value)
                    },
                    enabled = mobileState.valid.value && !timerState.isRunning && !loading,
                ) {
                    Text(text = timerState.text.value)
                }
            }
        )

        Spacer(modifier = Modifier.height(Dimens.dp16))
        BlockButton(
            enabled = formValid && !loading,
            onClick = {
                onSave(
                    MobileFormParams(
                        mobile = mobileState.field.value,
                        code = codeState.field.value,
                    )
                )
            }
        )
    }
}
