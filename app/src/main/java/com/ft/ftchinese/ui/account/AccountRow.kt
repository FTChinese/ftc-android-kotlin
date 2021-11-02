package com.ft.ftchinese.ui.account

import android.content.Context
import com.ft.ftchinese.R
import com.ft.ftchinese.store.AccountCache

data class AccountRow(
    val id: AccountRowType,
    val primary: String,
    val secondary: String
)

enum class AccountRowType {
    EMAIL,
    MOBILE,
    USER_NAME,
    PASSWORD,
    Address,
    STRIPE,
    WECHAT,
    DELETE
}

fun buildAccountRows(ctx: Context): List<AccountRow> {
    val account = AccountCache.get() ?: return listOf()

    return listOf(
        AccountRow(
            id = AccountRowType.EMAIL,

            primary = when {
                // For mobile-created account, or verified real email
                account.isMobileEmail ||
                account.isVerified -> ctx.getString(R.string.label_email)
                // otherwise show a not verified message.
                else -> ctx.getString(R.string.email_not_verified)
            },
            secondary =
            if (account.email.isNotBlank() && !account.isMobileEmail) {
                account.email
            } else {
                ctx.getString(R.string.default_not_set)
            }
        ),
        AccountRow(
            id = AccountRowType.USER_NAME,
            primary = ctx.getString(R.string.label_user_name),
            secondary = if (account.userName.isNullOrBlank()) {
                ctx.getString(R.string.default_not_set)
            } else {
                account.userName
            }
        ),
        AccountRow(
            id = AccountRowType.PASSWORD,
            primary = ctx.getString(R.string.label_password),
            secondary = "********",
        ),
        AccountRow(
            id = AccountRowType.Address,
            primary = "地址",
            secondary = "设置或更改地址",
        ),
        AccountRow(
            id = AccountRowType.STRIPE,
            primary = "Stripe钱包",
            secondary = "添加银行卡或设置默认支付方式"
        ),
        AccountRow(
            id = AccountRowType.WECHAT,
            primary = ctx.getString(R.string.label_wechat),
            secondary = if (account.isLinked) {
                ctx.getString(R.string.action_bound_account)
            } else {
                ctx.getString(R.string.action_bind_account)
            }
        ),
        AccountRow(
            id = AccountRowType.MOBILE,
            primary = "手机号",
            secondary = account.mobile ?: ctx.getString(R.string.default_not_set)
        ),
    )
}
