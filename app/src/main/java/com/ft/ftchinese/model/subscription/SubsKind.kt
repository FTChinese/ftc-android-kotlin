package com.ft.ftchinese.model.subscription

enum class SubsKind {
    Create, // Also applicable to expired membership.
    Renew,
    SwitchToYear,
    UpgradeToPrm,
    StdAddOn,
    PremAddOn
}

/**
 * Describe what kind of action an member could take for
 * next step.
 */
data class NextSteps(
    val subsKinds: List<SubsKind>,
    val message: String? = null,
) {
    val canReSubscribe: Boolean
        get() = subsKinds.contains(SubsKind.Create)

    val canRenew: Boolean
        get() = subsKinds.contains(SubsKind.Renew)

    val canSwitchCycle: Boolean
        get() = subsKinds.contains(SubsKind.SwitchToYear)

    val canUpgradePrm: Boolean
        get() = subsKinds.contains(SubsKind.UpgradeToPrm)

    val canStdAddOn: Boolean
        get() = subsKinds.contains(SubsKind.StdAddOn)

    val canPrmAddOn: Boolean
        get() = subsKinds.contains(SubsKind.PremAddOn)
}
