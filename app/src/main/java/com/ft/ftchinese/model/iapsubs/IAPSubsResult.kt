package com.ft.ftchinese.model.iapsubs

import com.ft.ftchinese.model.reader.Membership
import kotlinx.serialization.Serializable

@Serializable
data class IAPSubsResult(
    val subscription: IapSubs,
    val membership: Membership,
)
