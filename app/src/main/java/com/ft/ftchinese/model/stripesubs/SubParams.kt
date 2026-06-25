package com.ft.ftchinese.model.stripesubs

import com.ft.ftchinese.model.fetch.marshaller
import com.ft.ftchinese.model.paywall.MoneyParts
import com.ft.ftchinese.model.paywall.convertCent
import com.ft.ftchinese.model.paywall.getCurrencySymbol
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
data class SubParams(
    val priceId: String,
    val introductoryPriceId: String?,
    val defaultPaymentMethod: String?,
    val currency: String? = null,
    val coupon: String? = null,
    val ccode: String? = null,
    val from: String? = null,
    val idempotency: String? = null,
    val prorationDate: Long? = null,
) {
    fun toJsonString(): String {
        return marshaller.encodeToString(this)
    }
}

@Serializable
data class StripeInvoicePreviewLine(
    val id: String = "",
    val amount: Int = 0,
    val currency: String = "",
    val description: String = "",
    val proration: Boolean = false,
    val periodStart: Long = 0,
    val periodEnd: Long = 0,
)

@Serializable
data class StripeInvoicePreview(
    val currency: String = "",
    val amountDue: Int = 0,
    val subtotal: Int = 0,
    val total: Int = 0,
    val prorationDate: Long = 0,
    val lines: List<StripeInvoicePreviewLine> = emptyList(),
) {
    fun amountDueMoney(): MoneyParts {
        return MoneyParts(
            symbol = getCurrencySymbol(currency),
            amount = convertCent(amountDue),
        )
    }
}
