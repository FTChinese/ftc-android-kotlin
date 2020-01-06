package com.ft.ftchinese.model

import com.ft.ftchinese.model.content.Navigation
import com.ft.ftchinese.repository.Fetch
import org.junit.Test

class NavigationTest {
    @Test
    fun homePage() {
        val content = Fetch().get(Navigation.newsPages[0].contentUrl)
                .responseString()

        println(content)
    }
}
