package com.ft.ftchinese.model.subscription

import com.ft.ftchinese.R

enum class PayMethod(val symbol: String) {
    ALIPAY("alipay"),
    WXPAY("wechat"),
    STRIPE("stripe"),
    APPLE("apple"),
    B2B("b2b");

    val stringRes: Int
        get() = when (this) {
            ALIPAY -> R.string.pay_method_ali
            WXPAY -> R.string.pay_method_wechat
            STRIPE -> R.string.pay_method_stripe
            APPLE -> R.string.subs_brand_apple
            B2B -> R.string.subs_brand_b2b
        }

    override fun toString(): String {
        return symbol
    }

    companion object {

        private val stringToEnum: Map<String, PayMethod> = values().associateBy { it.symbol }

        @JvmStatic
        fun fromString(symbol: String?): PayMethod? {
            return stringToEnum[symbol]
        }
    }
}
