package com.ft.ftchinese.model.subscription

import android.os.Parcelable
import com.ft.ftchinese.model.fetch.KCycle
import com.ft.ftchinese.model.fetch.KTier
import kotlinx.parcelize.Parcelize

@Parcelize
data class Edition(
    @KTier
    val tier: Tier,
    @KCycle
    val cycle: Cycle
) : Parcelable
