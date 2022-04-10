package com.ft.ftchinese.ui.wxlink

import android.content.Context
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.enums.LoginMethod
import com.ft.ftchinese.model.reader.Membership

data class WxEmailLink(
    val ftc: Account,
    val wx: Account,
    val loginMethod: LoginMethod,
) {
    private fun pickMember(): Membership {
        if (ftc.membership.tier == null && wx.membership.tier == null) {
            return Membership()
        }

        if (ftc.membership.expireDate == null && wx.membership.expireDate == null) {
            return Membership()
        }

        if (ftc.membership.expireDate == null) {
            return wx.membership
        }

        if (wx.membership.expireDate == null) {
            return ftc.membership
        }

        if (ftc.membership.expireDate.isAfter(wx.membership.expireDate)) {
            return ftc.membership
        }

        return wx.membership
    }

    fun link(context: Context): LinkResult {
        if (ftc.isEqual(wx)) {
            return LinkResult(
                linked = null,
                denied = context.getString(R.string.accounts_already_linked),
            )
        }

        if (ftc.isLinked) {
            return LinkResult(
                linked = null,
                denied = context.getString(R.string.ftc_account_linked, ftc.email),
            )
        }

        if (wx.isLinked) {
            return LinkResult(
                linked = null,
                denied = context.getString(R.string.wx_account_linked, wx.wechat.nickname),
            )
        }

        if (!ftc.membership.autoRenewOffExpired && !wx.membership.autoRenewOffExpired) {
            return LinkResult(
                linked = null,
                denied = context.getString(R.string.accounts_member_valid)
            )
        }

        return LinkResult(
            linked = Account(
                id = ftc.id,
                unionId = wx.unionId,
                userName = ftc.userName,
                email = ftc.email,
                mobile = ftc.mobile,
                isVerified = ftc.isVerified,
                avatarUrl = ftc.avatarUrl,
                loginMethod = loginMethod,
                wechat = wx.wechat,
                membership = pickMember()
            ),
            denied = null,
        )
    }
}

data class LinkResult(
    val linked: Account?,
    val denied: String?, // Denial reason
)
