package com.ft.ftchinese.ui.validator

// A predicate is a function that evaluates to true when its params matches the condition of the predicate.
// When returning false, it indicates the param is not valid.
typealias Predicate = (value: String?) -> Boolean

data class ValidationRule(
    val predicate: Predicate,
    val message: String,
)

fun passwordRules(repeat: Boolean = false): List<ValidationRule> {
    return listOf(
        ValidationRule(
            predicate = Validator::notEmpty,
            message = if (repeat) {
                "确认密码不能为空"
            } else {
                "新密码不能为空"
           },
        ),
        ValidationRule(
            predicate = Validator.minLength(8),
            message = "长度不能少于8位"
        )
    )
}

fun requiredRule(msg: String): ValidationRule {
    return ValidationRule(
        predicate = Validator::notEmpty,
        message = msg
    )
}

val rulePasswordRequired = requiredRule("必须输入当前密码")
