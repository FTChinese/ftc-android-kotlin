package com.ft.ftchinese.ui.account

data class UIBankCard(
    val brand: String? = null,
    val number: String? = null
)

val cardBrands = mapOf(
        "amex" to "American Express",
        "discover" to "Discover",
        "jcb" to "JCB",
        "diners" to "Diners Club",
        "visa" to "Visa",
        "mastercard" to "MasterCard",
        "unionpay" to "UnionPay"
)
