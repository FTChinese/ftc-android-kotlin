package com.ft.ftchinese.model.subscription

import android.os.Parcelable
import com.ft.ftchinese.util.KDateTime
import kotlinx.android.parcel.Parcelize
import org.threeten.bp.ZonedDateTime

@Parcelize
data class Discount(
    val id: String? = null,
    val priceOff: Double? = null,
    val percent: Int? = null,
    @KDateTime
    val startUtc: ZonedDateTime? = null,
    @KDateTime
    val endUtc: ZonedDateTime? = null
) : Parcelable
