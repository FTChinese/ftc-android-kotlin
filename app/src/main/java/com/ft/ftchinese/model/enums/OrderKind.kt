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
    SwitchCycle("switch_cycle"); // Not from server. Use only locally for Stripe.

    val stringRes: Int
        get() = when (this) {
            Create -> R.string.order_kind_create
            Renew -> R.string.order_kind_renew
            Upgrade -> R.string.order_kind_upgrade
            AddOn -> R.string.order_kind_addon
            SwitchCycle -> R.string.order_kind_switch_cycle
        }

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


