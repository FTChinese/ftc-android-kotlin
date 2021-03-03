package com.ft.ftchinese.model.iapsubs

import com.ft.ftchinese.model.reader.Membership

data class IAPSubsResult(
    val subscription: Subscription,
    val membership: Membership,
)
