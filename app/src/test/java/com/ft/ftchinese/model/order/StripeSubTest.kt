package com.ft.ftchinese.model.order

import com.ft.ftchinese.model.fetch.json
import org.junit.Test

private val data = """
{
	"cancelAtPeriodEnd": false,
	"created": "2019-07-16T11:52:30Z",
	"currentPeriodEnd": "2020-07-16T11:52:30Z",
	"currentPeriodStart": "2019-07-16T11:52:30Z",
	"endedAt": null,
	"latestInvoice": {
		"created": "2019-07-16T11:52:30Z",
		"currency": "gbp",
		"hostedInvoiceUrl": "https://pay.stripe.com/invoice/invst_7w4r7nLr0fylK5nMaG7nVEE6Zs",
		"invoicePdf": "https://pay.stripe.com/invoice/invst_7w4r7nLr0fylK5nMaG7nVEE6Zs/pdf",
		"number": "6C9D489E-0007",
		"paid": true,
		"paymentIntent": {
			"clientSecret": "pi_1EwpCsBzTK0hABgJWF2EOnbd_secret_OmJNq8nSigylnFBBnCGMoNjqs",
			"status": "succeeded"
		},
		"receiptNumber": "2517-0427"
	},
	"startDate": "2019-07-16T11:52:30Z",
	"status": "active"
} 
""".trimIndent()

class StripeSubTest {
    @Test fun parseJson() {
        val stripeSub = json.parse<StripeSub>(data)

        println(stripeSub)
    }
}
