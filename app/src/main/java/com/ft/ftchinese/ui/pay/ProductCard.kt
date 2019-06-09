package com.ft.ftchinese.ui.pay

import android.os.Parcelable
import com.ft.ftchinese.model.Tier
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ProductCard(
        val tier: Tier,
        val heading: String,
        val description: String,
        val smallPrint: String? = null,
        val yearPrice: String,
        val monthPrice: String? = null
) : Parcelable
