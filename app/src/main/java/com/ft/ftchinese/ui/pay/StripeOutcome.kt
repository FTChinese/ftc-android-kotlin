package com.ft.ftchinese.ui.pay

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class StripeOutcome(
        val invoice: String,
        val plan: String,
        val period: String,
        val subStatus: String,
        val paymentStatus: String
) : Parcelable
