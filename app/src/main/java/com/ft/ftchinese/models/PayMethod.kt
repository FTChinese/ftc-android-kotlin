package com.ft.ftchinese.models

private val payMethodNames = arrayOf("alipay", "wechat", "stripe")
private val payMethodValue = mapOf(
        "alipay" to PayMethod.ALIPAY,
        "wechat" to PayMethod.WXPAY,
        "tenpay" to PayMethod.WXPAY, // Backward compatibility.
        "stripe" to PayMethod.STRIPE
)

enum class PayMethod {
    ALIPAY,
    WXPAY,
    STRIPE;

    fun string(): String {
        if (ordinal >= payMethodNames.size) {
            return ""
        }
        return payMethodNames[ordinal]
    }

    companion object {
        fun fromString(s: String?): PayMethod? {
            return payMethodValue[s]
        }
    }
}