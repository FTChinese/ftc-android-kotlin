package com.ft.ftchinese.model.enums

import android.os.Parcelable
import com.ft.ftchinese.R
import kotlinx.parcelize.Parcelize

@Parcelize
enum class OrderKind(val code: String) : Parcelable {
    Create("create"),
    Renew("renew"),
    Upgrade("upgrade"),
    AddOn("add_on"),
    Downgrade("downgrade"),
    SwitchCycle("switch_cycle"); // Not from server. Use only locally for Stripe.

    override fun toString(): String {
        return code
    }

    companion object{

        private val STRING_TO_ENUM: Map<String, OrderKind> = values().associateBy {
            it.code
        }

        @JvmStatic
        fun fromString(symbol: String?): OrderKind? {
            if (symbol == null) {
                return null
            }
            return STRING_TO_ENUM[symbol]
        }
    }
}


