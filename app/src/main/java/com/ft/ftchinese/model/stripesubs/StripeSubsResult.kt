package com.ft.ftchinese.model.stripesubs

import com.ft.ftchinese.model.reader.Membership
import kotlinx.serialization.Serializable

@Serializable
data class StripeSubsResult(
    val subs: Subscription,
    val membership: Membership
)
