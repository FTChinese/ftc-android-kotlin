package com.ft.ftchinese.ui.pay

import android.os.Parcelable
import com.ft.ftchinese.model.order.Tier
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PaywallProduct(
        val tier: Tier,
        val heading: String,
        val description: String,
        val smallPrint: String? = null,
        val yearPrice: String,
        val monthPrice: String? = null
) : Parcelable
