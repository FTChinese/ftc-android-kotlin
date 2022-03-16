package com.ft.ftchinese.model.paywall

import android.os.Parcelable
import com.ft.ftchinese.model.enums.OrderKind
import kotlinx.parcelize.Parcelize

/**
 * Used to pass as parcel.
 */
@Parcelize
data class StripePriceIDs(
    val orderKind: OrderKind,
    val recurring: String,
    val trial: String?,
) : Parcelable
