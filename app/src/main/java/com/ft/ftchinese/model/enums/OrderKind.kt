package com.ft.ftchinese.model.enums

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
enum class OrderKind(val code: String) : Parcelable {
    @SerialName("create")
    Create("create"),
    @SerialName("renew")
    Renew("renew"),
    @SerialName("upgrade")
    Upgrade("upgrade"),
    @SerialName("add_on")
    AddOn("add_on"),
    @SerialName("downgrade")
    Downgrade("downgrade"),
    @SerialName("switch_cycle")
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


