package com.ft.ftchinese.model

import com.ft.ftchinese.util.Fetch
import org.junit.Assert.*
import org.junit.Test

class NavigationTest {
    @Test
    fun homePage() {
        val content = Fetch().get(Navigation.newsPages[0].contentUrl)
                .responseString()

        println(content)
    }
}
