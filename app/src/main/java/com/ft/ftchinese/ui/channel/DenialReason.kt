package com.ft.ftchinese.ui.channel

import android.content.Context
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.Access
import com.ft.ftchinese.model.reader.MemberStatus
import com.ft.ftchinese.model.reader.Permission

data class DenialReason(
    val btnText: String,
    val prompt: String,
) {
    companion object {
        @JvmStatic
        fun from(ctx: Context, denied: Access): DenialReason? {
            return when (denied.status) {
                MemberStatus.NotLoggedIn -> DenialReason(
                    btnText = ctx.getString(R.string.btn_login),
                    prompt = ctx.getString(R.string.prompt_login_to_read),
                )
                MemberStatus.Empty -> {
                    val prefix = if (denied.content == Permission.PREMIUM) {
                        ctx.getString(R.string.restricted_to_premium)
                    } else {
                        ctx.getString(R.string.restricted_to_member)
                    }

                    DenialReason(
                        btnText = ctx.getString(R.string.btn_subscribe_now),
                        prompt =  "$prefix，${ctx.getString(R.string.not_subscribed_yet)}"
                    )
                }
                MemberStatus.Expired -> {
                    val prefix = if (denied.content == Permission.PREMIUM) {
                        ctx.getString(R.string.restricted_to_premium)
                    } else {
                        ctx.getString(R.string.restricted_to_member)
                    }

                    DenialReason(
                        btnText = ctx.getString(R.string.btn_subscribe_now),
                        prompt = "$prefix，${ctx.getString(R.string.subscription_expired)}"
                    )
                }
                MemberStatus.ActiveStandard -> if (denied.content == Permission.PREMIUM) {
                    DenialReason(
                        btnText = ctx.getString(R.string.btn_upgrade_now),
                        prompt = "${ctx.getString(R.string.restricted_to_premium)}，${ctx.getString(R.string.current_is_standard)}"
                    )
                } else null
                else -> null
            }
        }
    }
}
