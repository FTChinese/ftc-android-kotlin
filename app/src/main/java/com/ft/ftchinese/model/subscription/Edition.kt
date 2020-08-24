package com.ft.ftchinese.model.subscription

import android.os.Parcelable
import com.ft.ftchinese.util.KCycle
import com.ft.ftchinese.util.KTier
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Edition(
    @KTier
    val tier: Tier,
    @KCycle
    val cycle: Cycle
) : Parcelable
