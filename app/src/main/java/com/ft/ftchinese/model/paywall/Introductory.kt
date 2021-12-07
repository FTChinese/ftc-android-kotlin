package com.ft.ftchinese.model.paywall

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Introductory(
    val stripePriceId: String?,
): Parcelable
