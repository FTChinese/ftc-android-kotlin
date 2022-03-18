package com.ft.ftchinese.model.paywall

enum class IntentKind {
    Forbidden,
    Create,
    Renew,
    Upgrade,
    Downgrade,
    AddOn,
    OneTimeToAutoRenew,
    SwitchInterval
}
