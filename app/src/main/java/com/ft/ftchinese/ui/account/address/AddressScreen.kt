package com.ft.ftchinese.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.ft.ftchinese.model.reader.Address
import com.ft.ftchinese.ui.components.BlockButton
import com.ft.ftchinese.ui.components.TextInput
import com.ft.ftchinese.ui.components.rememberInputState
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.validator.ValidationRule
import com.ft.ftchinese.ui.validator.Validator

@Composable
fun AddressScreen(
    address: Address?,
    loading: Boolean,
    onSave: (Address) -> Unit,
) {
    val provinceState = rememberInputState(
        initialValue = address?.province ?: "",
        rules = buildRules(8),
    )

    val cityState = rememberInputState(
        initialValue = address?.city ?: "",
        rules = buildRules(16),
    )

    val districtState = rememberInputState(
        initialValue = address?.district ?: "",
        rules = buildRules(16),
    )

    val streetState = rememberInputState(
        initialValue = address?.street ?: "",
        rules = buildRules(128),
    )

    val postCodeState = rememberInputState(
        initialValue = address?.postcode ?: "",
        rules = buildRules(16),
    )

    val formValid = provinceState.valid.value && cityState.valid.value && districtState.valid.value && streetState.valid.value && postCodeState.valid.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.dp16)
    ) {
        TextInput(
            label = "省/直辖市",
            state = provinceState,
        )

        TextInput(
            label = "市",
            state = cityState,
        )

        TextInput(
            label = "区/县",
            state = districtState,
        )

        TextInput(
            label = "街道、门牌号",
            state = streetState,
        )

        TextInput(
            label = "邮编",
            state = postCodeState,
            keyboardType = KeyboardType.Number,
        )

        Spacer(modifier = Modifier.height(Dimens.dp16))

        BlockButton(
            enabled = formValid && !loading,
            onClick = {
                onSave(
                    Address(
                    province = provinceState.field.value,
                    city = cityState.field.value,
                    district = districtState.field.value,
                    street = streetState.field.value,
                    postcode = postCodeState.field.value
                ))
            }
        )
    }
}

private fun buildRules(max: Int): List<ValidationRule> {
    return listOf(
        ValidationRule(
            predicate = Validator::notEmpty,
            message = ""
        ),
        ValidationRule(
            predicate = Validator.maxLength(max),
            message = "输入内容过长",
        ),
    )
}
