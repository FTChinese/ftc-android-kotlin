package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.ft.ftchinese.ui.theme.OColor
import com.ft.ftchinese.ui.validator.ValidationRule

class InputState(
    initialValue: String = "",
    private val rules: List<ValidationRule> = listOf()
) {

    val field = mutableStateOf(initialValue)
    val touched = mutableStateOf(false)
    val error = mutableStateOf("")
    val valid = derivedStateOf {
        validate()
    }

    private fun validate(): Boolean {
        for (rule in rules) {
            if (!rule.predicate(field.value)) {
                error.value = rule.message
                return false
            }
        }

        return true
    }

    fun onValueChanged(newValue: String) {
        touched.value = newValue.isNotBlank()
        field.value = newValue
    }
}

@Composable
fun rememberInputState(
    initialValue: String = "",
    rules: List<ValidationRule> = listOf()
) = remember(initialValue) {
    InputState(
        initialValue = initialValue,
        rules = rules
    )
}

@Composable
fun TextInput(
    label: String,
    state: InputState,
    enabled: Boolean = true,
    readOnly: Boolean = false,
) {
    val isError = remember(state.touched, state.valid) {
        derivedStateOf { state.touched.value && !state.valid.value }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = state.field.value,
            onValueChange = state::onValueChanged,
            label = {
                Text(
                    text = label
                )
            },
            isError = isError.value,
            modifier = Modifier.fillMaxWidth(),
            readOnly = readOnly,
            enabled = enabled,
        )
        if (isError.value) {
            Text(
                text = state.error.value,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.body2,
                color = OColor.claret,
            )
        }
    }
}
