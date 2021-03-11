package com.ft.ftchinese.model.ftcsubs

import com.ft.ftchinese.model.invoice.Invoice
import com.ft.ftchinese.model.reader.Membership

data class Invoices(
    val purchased: Invoice,
    val carriedOver: Invoice? = null,
) {
    fun membership(current: Membership): Membership {
        val newMember = current.withInvoice(purchased)

        if (carriedOver == null) {
            return newMember
        }

        return newMember.withInvoice(carriedOver)
    }
}
