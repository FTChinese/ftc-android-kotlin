package com.ft.ftchinese.ui.account

data class AccountRow(
    val id: AccountRowType,
    val primary: String,
    val secondary: String
)

enum class AccountRowType {
    EMAIL,
    USER_NAME,
    PASSWORD,
    Address,
    STRIPE,
    WECHAT
}
