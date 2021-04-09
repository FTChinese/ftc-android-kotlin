package com.ft.ftchinese.ui.mobile

enum class MobileFormField {
    Phone,
    Code;
}

data class MobileFormState(
    val errMsg: Int? = null,
    val errField: MobileFormField? = null,
    val mobileValid: Boolean = false,
    val codeValid: Boolean = false,
)

