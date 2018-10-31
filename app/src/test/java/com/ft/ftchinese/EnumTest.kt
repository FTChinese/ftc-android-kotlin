package com.ft.ftchinese

import com.ft.ftchinese.models.AdParser
import com.ft.ftchinese.models.AdPosition
import org.junit.Test

class EnumTest {
    @Test fun enumValues() {
        println(AdPosition.TOP_BANNER.name)

        enumValues<AdPosition>().forEach {
            val adCode = AdParser.getAdCode(it)

            print(adCode)
            print(it.position)
        }
    }
}