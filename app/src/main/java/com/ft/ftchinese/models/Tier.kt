package com.ft.ftchinese.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

private val tierNames = arrayOf("standard", "premium")
private val tierValue = mapOf(
        "standard" to Tier.STANDARD,
        "premium" to Tier.PREMIUM
)

@Parcelize
enum class Tier : Parcelable {
    STANDARD,
    PREMIUM;

    fun string(): String {
        if (ordinal >= tierNames.size) {
            return ""
        }
        return tierNames[ordinal]
    }

    companion object {
        fun fromString(s: String?): Tier? {
            return tierValue[s]
        }
    }
}