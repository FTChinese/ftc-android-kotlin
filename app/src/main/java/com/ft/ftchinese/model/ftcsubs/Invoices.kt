package com.ft.ftchinese.model.ftcsubs

import com.ft.ftchinese.model.invoice.Invoice
import com.ft.ftchinese.model.reader.Membership

data class Invoices(
    val purchased: Invoice,
    val carriedOver: Invoice? = null,
) {
    // Build new membership based on current one.
    fun membership(current: Membership): Membership {
        val newMember = current.withInvoice(purchased)

        if (carriedOver == null) {
            return newMember
        }

        return newMember.withInvoice(carriedOver)
    }
}
