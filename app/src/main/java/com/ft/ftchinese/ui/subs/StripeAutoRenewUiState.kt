package com.ft.ftchinese.ui.subs

import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.enums.StripeSubStatus
import com.ft.ftchinese.model.reader.Membership

data class StripeAutoRenewUiState(
    val visible: Boolean,
    val checked: Boolean,
    val title: String,
    val status: String,
    val detail: String,
    val offConfirmation: String,
)

fun Membership.stripeAutoRenewUiState(preferredLanguage: String = "zh"): StripeAutoRenewUiState {
    val zh = preferredLanguage.startsWith("zh", ignoreCase = true)
    val manageableStatus = status == null ||
        status == StripeSubStatus.Active ||
        status == StripeSubStatus.Trialing ||
        status == StripeSubStatus.Canceled
    val visible = !vip &&
        payMethod == PayMethod.STRIPE &&
        !stripeSubsId.isNullOrBlank() &&
        !expired &&
        manageableStatus

    if (!visible) {
        return StripeAutoRenewUiState(
            visible = false,
            checked = false,
            title = "",
            status = "",
            detail = "",
            offConfirmation = "",
        )
    }

    val expiry = localizeExpireDate()
    val currentTier = tier?.label(zh).orEmpty()
    val pendingDowngrade = autoRenew && pendingStripeChange?.isDowngrade == true
    val nextTier = if (pendingDowngrade) {
        pendingStripeChange?.targetTier?.label(zh).orEmpty()
    } else {
        currentTier
    }

    val title = if (zh) {
        "信用卡/借记卡自动续订"
    } else {
        "Card auto-renewal"
    }

    val status = when {
        zh && autoRenew && nextTier.isNotBlank() -> "已开启 · 下次续订：$nextTier"
        zh && autoRenew -> "已开启"
        zh -> "已关闭"
        autoRenew && nextTier.isNotBlank() -> "On · Next renewal: $nextTier"
        autoRenew -> "On"
        else -> "Off"
    }

    val detail = when {
        zh && pendingDowngrade && currentTier.isNotBlank() && nextTier.isNotBlank() ->
            "当前${currentTier}权益保留至$expiry，下个计费周期起按${nextTier}自动续订。"
        zh && autoRenew && currentTier.isNotBlank() ->
            "当前${currentTier}权益有效至$expiry，到期后将继续自动续订。"
        zh ->
            "当前权益保留至$expiry，到期后不再续订。"
        pendingDowngrade && currentTier.isNotBlank() && nextTier.isNotBlank() ->
            "Your $currentTier access remains until $expiry. The next billing cycle renews as $nextTier."
        autoRenew && currentTier.isNotBlank() ->
            "Your $currentTier access is valid until $expiry and will auto-renew."
        else ->
            "Access remains until $expiry and will not renew."
    }

    val offConfirmation = when {
        zh && pendingDowngrade ->
            "关闭自动续订后，会取消已安排的下次降级，并在当前周期结束后停止续订。当前权益仍保留至$expiry。"
        zh ->
            "关闭自动续订后，当前权益仍保留至$expiry，到期后不再续订。您可以在到期前重新开启。"
        pendingDowngrade ->
            "Turning off auto-renewal cancels the scheduled downgrade and stops renewal at the end of the current period. Access remains until $expiry."
        else ->
            "Turning off auto-renewal keeps access until $expiry. You can turn it back on before expiry."
    }

    return StripeAutoRenewUiState(
        visible = true,
        checked = autoRenew,
        title = title,
        status = status,
        detail = detail,
        offConfirmation = offConfirmation,
    )
}

private fun com.ft.ftchinese.model.enums.Tier.label(zh: Boolean): String {
    return when (this.name.lowercase()) {
        "premium" -> if (zh) "高端会员" else "Premium"
        "standard" -> if (zh) "标准会员" else "Standard"
        else -> this.name
    }
}
