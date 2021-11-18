package com.ft.ftchinese.model.ftcsubs

import com.ft.ftchinese.model.fetch.json
import org.junit.Test


class DiscountTest {
    private val data = """
    {
        "id": "dsc_ytFW0fkWYipx",
        "priceOff": 100,
        "percent": null,
        "startUtc": "2020-10-19T16:00:00Z",
        "endUtc": "2020-10-30T16:00:00Z",
        "description": null
    }
    """.trimIndent()
    @Test
    fun parseJSON() {
        val d = json.parse<Discount>(data)
        println(d)
    }
}
