package com.ft.ftchinese.model.stripesubs

import com.ft.ftchinese.model.reader.Membership

data class StripeSubsResult(
    val subs: Subscription,
    val membership: Membership
)
