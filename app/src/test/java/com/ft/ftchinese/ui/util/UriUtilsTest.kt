package com.ft.ftchinese.ui.util

import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.model.enums.ArticleType
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.reader.Wechat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UriUtilsTest {

    @Test
    fun radioTeaserUsesFullContentPageForArticleScreen() {
        val url = UriUtils.teaserUrl(
            teaser = dailyWordTeaser(),
            account = standardAccount()
        )

        assertTrue(url!!.startsWith("https://wwwftchineseandroid001.scdn8.secure.raxcdn.com/interactive/257134"))
        assertTrue(url.contains("webview=ftcapp"))
        assertFalse(url.contains("bodyonly=yes"))
        assertFalse(url.contains("www.ftchinese.com"))
    }

    @Test
    fun radioTeaserAudioScreenKeepsBodyOnlyPage() {
        val url = UriUtils.teaserAudioPageUrl(
            teaser = dailyWordTeaser(),
            account = standardAccount()
        )

        assertTrue(url!!.startsWith("https://wwwftchineseandroid001.scdn8.secure.raxcdn.com/interactive/257134"))
        assertTrue(url.contains("bodyonly=yes"))
        assertFalse(url.contains("www.ftchinese.com"))
    }

    @Test
    fun contentAudioTeaserUsesProtectedAudioRoute() {
        val url = UriUtils.teaserAudioPageUrl(
            teaser = Teaser(
                id = "716d0651-6f1f-4215-9d98-96a5047539cd",
                title = "测试文章",
                type = ArticleType.Content,
                langVariant = com.ft.ftchinese.model.content.Language.ENGLISH,
            ),
            account = standardAccount()
        )

        assertTrue(url!!.contains("/content/audio/en/716d0651-6f1f-4215-9d98-96a5047539cd"))
        assertTrue(url.contains("webview=ftcapp"))
        assertTrue(url.contains("for=audio"))
    }

    private fun dailyWordTeaser(): Teaser {
        return Teaser(
            id = "257134",
            title = "每日一词：CANNIBALISE",
            type = ArticleType.Interactive,
            subType = Teaser.SUB_TYPE_RADIO,
            radioUrl = "https://audio.ftcn.net.cn/album/a_1773935578_5120.mp3"
        )
    }

    private fun standardAccount(): Account {
        return Account(
            id = "test-user",
            email = "test@example.com",
            wechat = Wechat(),
            membership = Membership(tier = Tier.STANDARD)
        )
    }
}
