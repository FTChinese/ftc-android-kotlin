package com.ft.ftchinese.ui.pay

import com.ft.ftchinese.model.reader.NextStep

data class UIMemberInfo(
        val tier: String,
        val expireDate: String,
        val autoRenewal: Boolean,
        val stripeStatus: String?, // Show stripe status or hide it if null
        val stripeInactive: Boolean, // Show stripe status warning
        val remains: String?, // Show remaining days that will expire or expired.
        val isValidIAP: Boolean
)

/**
 * Which buttons should present based on current membership
 * status.
 * For vip, show nothing;
 * For membership that is empty, expired, inactive stripe, show subscription button;
 * For active standard, show renewal button only if within renewal range, and always show upgrade button;
 * For active premium show renewal buttons if within renewal range, otherwise none;
 */
data class UIMemberNextSteps(
        val reSubscribe: Boolean,
        val renew: Boolean,
        val upgrade: Boolean
)

fun buildNextStepButtons(steps: Int): UIMemberNextSteps {
    return UIMemberNextSteps(
            reSubscribe = (steps and NextStep.Resubscribe.id) > 0,
            renew = (steps and NextStep.Renew.id) > 0,
            upgrade = (steps and NextStep.Upgrade.id) > 0
    )
}
