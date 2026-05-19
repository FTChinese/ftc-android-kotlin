package com.ft.ftchinese.ui.subs.member

import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.ui.subs.StripeAutoRenewUiState
import com.ft.ftchinese.ui.subs.stripeAutoRenewUiState

internal data class SubsOptionVisibility(
    val showStripeAutoRenewSwitch: Boolean,
    val stripeAutoRenewChecked: Boolean,
    val stripeAutoRenewUiState: StripeAutoRenewUiState?,
)

internal fun Membership.subsOptionVisibility(): SubsOptionVisibility {
    val autoRenewUiState = stripeAutoRenewUiState()

    return SubsOptionVisibility(
        showStripeAutoRenewSwitch = autoRenewUiState.visible,
        stripeAutoRenewChecked = autoRenewUiState.checked,
        stripeAutoRenewUiState = autoRenewUiState.takeIf { it.visible },
    )
}
