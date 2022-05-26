package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.ft.ftchinese.R
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
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
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
            trailingIcon = trailingIcon,
            isError = isError.value,
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                autoCorrect = false,
            ),
            modifier = Modifier.fillMaxWidth(),
            readOnly = readOnly,
            enabled = enabled,
            singleLine = true,
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

@Composable
fun PasswordInput(
    label: String,
    state: InputState,
    enabled: Boolean = true,
    readOnly: Boolean = false,
) {
    var (visible, setVisible) = rememberSaveable { mutableStateOf(false) }

    TextInput(
        label = label,
        state = state,
        enabled = enabled,
        readOnly = readOnly,
        keyboardType = KeyboardType.Password,
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            val image = if (visible) {
                painterResource(id = R.drawable.ic_baseline_visibility_24)
            } else {
                painterResource(id = R.drawable.ic_baseline_visibility_off_24)
            }

            val desc = if (visible) "Hide password" else "Show password"
            IconButton(
                onClick = { setVisible(!visible) }
            ) {
                Icon(
                    painter = image,
                    contentDescription = desc
                )
            }
        }
    )
}
