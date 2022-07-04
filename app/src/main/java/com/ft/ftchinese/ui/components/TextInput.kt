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

object OTextFieldDefaults {
    @Composable
    fun outlineTextFieldColors() = TextFieldDefaults.outlinedTextFieldColors(
        cursorColor = OColor.teal,
        focusedBorderColor = OColor.teal.copy(alpha = ContentAlpha.high),
        focusedLabelColor = OColor.teal,
    )
}

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
    singleLine: Boolean = true,
) {
    val isError = remember(state.touched, state.valid) {
        derivedStateOf { state.touched.value && !state.valid.value }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        InputField(
            label = label,
            state = state,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            readOnly = readOnly,
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardType = keyboardType,
            isError = isError.value,
            singleLine = singleLine,
        )
        if (isError.value) {
            InputInvalid(error = state.error.value)
        }
    }
}

@Composable
fun InputField(
    label: String,
    state: InputState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean,
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
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            autoCorrect = false,
        ),
        modifier = modifier,
        readOnly = readOnly,
        enabled = enabled,
        singleLine = singleLine,
        colors = OTextFieldDefaults.outlineTextFieldColors()
    )
}

@Composable
fun InputInvalid(
    error: String,
) {
    if (error.isNotBlank()) {
        Text(
            text = error,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.body2,
            color = OColor.claret,
        )
    }
}

@Composable
fun PasswordInput(
    label: String,
    state: InputState,
    enabled: Boolean = true,
    readOnly: Boolean = false,
) {
    val (visible, setVisible) = rememberSaveable { mutableStateOf(false) }

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
