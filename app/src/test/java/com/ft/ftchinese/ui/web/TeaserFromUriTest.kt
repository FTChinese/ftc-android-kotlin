package com.ft.ftchinese.ui.web

import android.net.Uri
import com.ft.ftchinese.model.content.Language
import com.ft.ftchinese.model.enums.ArticleType
import com.ft.ftchinese.tracking.PaywallTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TeaserFromUriTest {

    @Test
    fun parseInteractiveSubtypeFromQuery() {
        val teaser = teaserFromUrl("https://www.ftchinese.com/interactive/12345?subtype=bilingual&tier=premium")

        assertEquals(ArticleType.Interactive, teaser.type)
        assertEquals("bilingual", teaser.subType)
        assertTrue(teaser.hasJsAPI)
    }

    @Test
    fun normalizeLegacyFtArticleSubtype() {
        val teaser = teaserFromUrl("https://www.ftchinese.com/interactive/12345?subtype=FTArticle")

        assertEquals("bilingual", teaser.subType)
        assertTrue(teaser.hasJsAPI)
    }

    @Test
    fun interactiveWithoutSubtypeKeepsHtmlFallback() {
        val teaser = teaserFromUrl("https://www.ftchinese.com/interactive/12345?tier=free")

        assertNull(teaser.subType)
        assertFalse(teaser.hasJsAPI)
    }

    @Test
    fun ignoreSubtypeQueryForStoryLink() {
        val teaser = teaserFromUrl("https://www.ftchinese.com/story/12345?subtype=bilingual")

        assertEquals(ArticleType.Story, teaser.type)
        assertNull(teaser.subType)
        assertTrue(teaser.hasJsAPI)
    }

    @Test
    fun parseContentLinkAsNativeArticle() {
        val id = "2b2a9375-829c-4b4a-af98-c5e1e963c493"
        val teaser = teaserFromUrl("https://ai.chineseft.net/content/$id?tier=premium")

        assertEquals(ArticleType.Content, teaser.type)
        assertEquals(id, teaser.id)
        assertTrue(teaser.hasJsAPI)
        assertEquals("/api/content/$id", teaser.jsApiPath)
    }

    @Test
    fun parseChineseftContentLinkAsNativeArticle() {
        val id = "f3044cb3-081b-49e3-a79a-dbff0a4e7163"
        val teaser = teaserFromUrl("https://www.chineseft.net/content/$id?syn-25a6b1a6=1")

        assertEquals(ArticleType.Content, teaser.type)
        assertEquals(id, teaser.id)
        assertTrue(teaser.hasJsAPI)
        assertEquals("/api/content/$id", teaser.jsApiPath)
    }

    @Test
    fun routeFtContentLinkAsNativeArticle() {
        val id = "f3044cb3-081b-49e3-a79a-dbff0a4e7163"
        val event = WvUrlEvent.fromUri(Uri.parse("https://www.ft.com/content/$id?syn-25a6b1a6=1"))

        assertTrue(event is WvUrlEvent.Article)
        val teaser = (event as WvUrlEvent.Article).teaser
        assertEquals(ArticleType.Content, teaser.type)
        assertEquals(id, teaser.id)
        assertTrue(teaser.hasJsAPI)
        assertEquals("/api/content/$id", teaser.jsApiPath)
    }

    @Test
    fun routeFtaSubscriptionLinkAsNativePaywallAndTrackCcode() {
        val ccode = "android_native_ccode_test"
        PaywallTracker.from = null

        val event = WvUrlEvent.fromUri(
            Uri.parse("https://www.ftacademy.cn/subscription.html?ccode=$ccode")
        )

        assertTrue(event is WvUrlEvent.FtaSubs)
        assertEquals(ccode, PaywallTracker.campaignCcode())

        PaywallTracker.from = null
    }

    @Test
    fun routeBareFtaSubscriptionLinkAsNativePaywallAndTrackCcode() {
        val ccode = "android_native_bare_host_test"
        PaywallTracker.from = null

        val event = WvUrlEvent.fromUri(
            Uri.parse("https://ftacademy.cn/subscription.html?ccode=$ccode")
        )

        assertTrue(event is WvUrlEvent.FtaSubs)
        assertEquals(ccode, PaywallTracker.campaignCcode())

        PaywallTracker.from = null
    }

    @Test
    fun routeFtaSubscriptionPathWithoutHtmlAsNativePaywall() {
        val ccode = "android_native_subscription_path_test"
        PaywallTracker.from = null

        val event = WvUrlEvent.fromUri(
            Uri.parse("https://www.ftacademy.cn/subscription?ccode=$ccode")
        )

        assertTrue(event is WvUrlEvent.FtaSubs)
        assertEquals(ccode, PaywallTracker.campaignCcode())

        PaywallTracker.from = null
    }

    @Test
    fun parseLanguageSuffixWithoutLosingContentId() {
        val id = "2b2a9375-829c-4b4a-af98-c5e1e963c493"
        val teaser = teaserFromUrl("https://ai.chineseft.net/content/$id/ce?tier=premium")

        assertEquals(ArticleType.Content, teaser.type)
        assertEquals(id, teaser.id)
        assertEquals(Language.BILINGUAL, teaser.langVariant)
    }
}
