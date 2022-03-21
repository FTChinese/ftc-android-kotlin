package com.ft.ftchinese.model.paywall

import android.os.Parcelable
import com.ft.ftchinese.model.enums.OrderKind
import kotlinx.parcelize.Parcelize

/**
 * Used to pass as parcel.
 */
@Deprecated("")
@Parcelize
data class StripePriceIDs(
    val orderKind: OrderKind,
    val recurring: String,
    val trial: String?,
) : Parcelable

data class StripePriceIDsV2(
    val recurring: List<String>,
    val trial: String?,
) {
    companion object {
        fun newInstance(ftcItems: List<CartItemFtcV2>): StripePriceIDsV2 {
            var trial: String? = null
            val recur = mutableListOf<String>()

            ftcItems.forEach {
                if (it.isIntro) {
                    trial = it.price.stripePriceId
                } else {
                    recur.add(it.price.stripePriceId)
                }
            }

            return StripePriceIDsV2(
                recurring = recur,
                trial = trial,
            )
        }
    }
}
