package com.ft.ftchinese.model.subscription

import com.ft.ftchinese.model.fetch.json
import org.junit.Assert.*
import org.junit.Test

class StripePriceTest {
    private val data = """
        [
            {
                "id": "plan_FOdgPTznDwHU4i",
                "tier": "standard",
                "cycle": "month",
                "active": true,
                "currency": "gbp",
                "liveMode": false,
                "nickname": "Standard Monthly Plan",
                "productId": "prod_FOde1wE4ZTRMcD",
                "unitAmount": 390,
                "created": 1562567567
            },
            {
                "id": "plan_FOdfeaqzczp6Ag",
                "tier": "standard",
                "cycle": "year",
                "active": true,
                "currency": "gbp",
                "liveMode": false,
                "nickname": "Standard Yearly Plan",
                "productId": "prod_FOde1wE4ZTRMcD",
                "unitAmount": 3000,
                "created": 1562567504
            },
            {
                "id": "plan_FOde0uAr0V4WmT",
                "tier": "premium",
                "cycle": "year",
                "active": true,
                "currency": "gbp",
                "liveMode": false,
                "nickname": "Premium Yearly Plan",
                "productId": "prod_FOdd1iNT29BIGq",
                "unitAmount": 23800,
                "created": 1562567431
            }
        ]
    """.trimIndent()

}
