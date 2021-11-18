package com.ft.ftchinese.model.stripesubs

import android.os.Parcelable
import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.fetch.KCycle
import kotlinx.parcelize.Parcelize

@Parcelize
data class PriceRecurring(
    @KCycle
    val interval: Cycle? = null, // week, month, year
    val intervalCount: Int,
    val usageType: String, // licensed, metered
) : Parcelable
