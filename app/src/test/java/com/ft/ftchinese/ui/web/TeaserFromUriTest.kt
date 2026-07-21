package com.ft.ftchinese.ui.web

import android.net.Uri
import com.ft.ftchinese.model.content.Language
import com.ft.ftchinese.model.enums.ArticleType
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.tracking.PaywallTracker
import com.ft.ftchinese.ui.subs.SubscriptionEntryIntent
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
        val ftaSubs = event as WvUrlEvent.FtaSubs
        assertEquals(ccode, ftaSubs.ccode)
        assertNull(ftaSubs.from)

        PaywallTracker.from = null
    }

    @Test
    fun preserveFtaSubscriptionDiscountSource() {
        val event = WvUrlEvent.fromUri(
            Uri.parse(
                "https://www.ftacademy.cn/subscription.html" +
                    "?ccode=olivertest010&from=android_api_test_2026"
            )
        )

        assertTrue(event is WvUrlEvent.FtaSubs)
        val ftaSubs = event as WvUrlEvent.FtaSubs
        assertEquals("olivertest010", ftaSubs.ccode)
        assertEquals("android_api_test_2026", ftaSubs.from)

        PaywallTracker.from = null
    }

    @Test
    fun routeGamCampaignSubscriptionByNestedAdUrl() {
        val event = WvUrlEvent.fromUri(
            Uri.parse(
                "https://adclick.g.doubleclick.net/pcs/click" +
                    "?ccode=gam_outer_ccode" +
                    "&adurl=https%3A%2F%2Fwww.ftacademy.cn%2Fsubscription.html" +
                    "%3Fccode%3Dgam_landing_ccode"
            )
        )

        assertTrue(event is WvUrlEvent.CampaignAd)
        val campaign = event as WvUrlEvent.CampaignAd
        assertEquals("gam_landing_ccode", campaign.ccode)
        assertEquals("www.ftacademy.cn", campaign.landingUri?.host)
        assertEquals("/subscription.html", campaign.landingUri?.path)
    }

    @Test
    fun gamLinkWithoutCcodeRemainsExternal() {
        val event = WvUrlEvent.fromUri(
            Uri.parse(
                "https://adclick.g.doubleclick.net/pcs/click" +
                    "?adurl=https%3A%2F%2Fwww.ftacademy.cn%2Fsubscription.html"
            )
        )

        assertTrue(event is WvUrlEvent.External)
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
    fun routeSubscribeSchemeAsNativePaywallIntent() {
        val event = WvUrlEvent.fromUri(
            Uri.parse("subscribe://premium/1498?ccode=android_native_ccode&offer=premium_retention_offer")
        )

        assertTrue(event is WvUrlEvent.Subscribe)
        val subscribe = event as WvUrlEvent.Subscribe
        assertEquals(Tier.PREMIUM, subscribe.tier)
        assertEquals("android_native_ccode", subscribe.ccode)
        assertEquals("premium_retention_offer", subscribe.offerHint)
        assertEquals("1498", subscribe.priceHint)
        assertEquals("subscribe", subscribe.sourceScheme)
    }

    @Test
    fun rejectUnsafeSubscribeCampaignCode() {
        PaywallTracker.from = null

        val event = WvUrlEvent.fromUri(
            Uri.parse("subscribe://standard/258?ccode=%3Cscript%3E&offer=standard_safe_offer")
        )

        assertTrue(event is WvUrlEvent.Subscribe)
        val subscribe = event as WvUrlEvent.Subscribe
        assertEquals(Tier.STANDARD, subscribe.tier)
        assertNull(subscribe.ccode)
        assertEquals("standard_safe_offer", subscribe.offerHint)
        assertNull(SubscriptionEntryIntent(ccode = subscribe.ccode).campaignCcode())
    }

    @Test
    fun routeWebSubscriptionLinkAsNativePaywallIntent() {
        val event = WvUrlEvent.fromUri(
            Uri.parse("https://www.ftchinese.com/subscription.html?from=ft_full_price&ccode=olivertest012&tap=standard#no_universal_links")
        )

        assertTrue(event is WvUrlEvent.Subscribe)
        val subscribe = event as WvUrlEvent.Subscribe
        assertEquals(Tier.STANDARD, subscribe.tier)
        assertEquals("olivertest012", subscribe.ccode)
        assertEquals("ft_full_price", subscribe.from)
        assertNull(subscribe.offerHint)
        assertNull(subscribe.priceHint)
        assertEquals("web-subscription", subscribe.sourceScheme)
    }

    @Test
    fun routePremiumWebSubscriptionLinkAsNativePaywallIntent() {
        val event = WvUrlEvent.fromUri(
            Uri.parse("https://www.ftchinese.com/subscription.html?from=ft_renewal&ccode=renewal_ccode&tap=premium#no_universal_links")
        )

        assertTrue(event is WvUrlEvent.Subscribe)
        val subscribe = event as WvUrlEvent.Subscribe
        assertEquals(Tier.PREMIUM, subscribe.tier)
        assertEquals("renewal_ccode", subscribe.ccode)
        assertEquals("ft_renewal", subscribe.from)
        assertEquals("web-subscription", subscribe.sourceScheme)
    }

    @Test
    fun routeRelativeWebSubscriptionLinkAsNativePaywallIntent() {
        val event = WvUrlEvent.fromUri(
            Uri.parse("/subscription.html?from=ft_discount&ccode=relative_ccode&tap=premium#no_universal_links")
        )

        assertTrue(event is WvUrlEvent.Subscribe)
        val subscribe = event as WvUrlEvent.Subscribe
        assertEquals(Tier.PREMIUM, subscribe.tier)
        assertEquals("relative_ccode", subscribe.ccode)
        assertEquals("ft_discount", subscribe.from)
    }

    @Test
    fun ignoreWebSubscriptionPriceParam() {
        val event = WvUrlEvent.fromUri(
            Uri.parse("https://www.ftchinese.com/subscription.html?price=1&from=ft_discount&ccode=price_ignored&tap=premium")
        )

        assertTrue(event is WvUrlEvent.Subscribe)
        val subscribe = event as WvUrlEvent.Subscribe
        assertEquals(Tier.PREMIUM, subscribe.tier)
        assertEquals("ft_discount", subscribe.from)
        assertNull(subscribe.priceHint)
    }

    @Test
    fun routeCorpPageInsideAppWithWebviewParam() {
        val event = WvUrlEvent.fromUri(
            Uri.parse("https://www.ftchinese.com/m/corp/service.html?ad=no")
        )

        assertTrue(event is WvUrlEvent.CorpPage)
        val corpPage = event as WvUrlEvent.CorpPage
        assertEquals("/m/corp/service.html", corpPage.uri.path)
        assertEquals("no", corpPage.uri.getQueryParameter("ad"))
        assertEquals("ftcapp", corpPage.uri.getQueryParameter("webview"))
    }

    @Test
    fun routeCorpPreviewAsMarketingChannel() {
        val event = WvUrlEvent.fromUri(
            Uri.parse("https://www.ftchinese.com/m/corp/preview.html?pageid=events&webview=ftcapp")
        )

        assertTrue(event is WvUrlEvent.Channel)
        val channel = event as WvUrlEvent.Channel
        assertEquals("/m/corp/preview.html", channel.source.path)
        assertEquals("pageid=events&webview=ftcapp", channel.source.query)
    }

    @Test
    fun routeSafeModeLoginAsNativeLogin() {
        val event = WvUrlEvent.fromUri(
            Uri.parse("https://www.ftchinese.com/login/safe_mode?next=%2Fstory%2F001110189")
        )

        assertTrue(event is WvUrlEvent.Login)
    }

    @Test
    fun routeLoginAsNativeLoginOnTrustedAuthDomain() {
        val event = WvUrlEvent.fromUri(
            Uri.parse("https://www.chineseft.net/login")
        )

        assertTrue(event is WvUrlEvent.Login)
    }

    @Test
    fun routeFtcVicCorpPreviewAsMarketingChannel() {
        val event = WvUrlEvent.fromUri(
            Uri.parse("https://www.ftcvic.com/m/corp/preview.html?ccode=olivertest012&pageid=2026Junsub&to=all")
        )

        assertTrue(event is WvUrlEvent.Channel)
        val channel = event as WvUrlEvent.Channel
        assertEquals("/m/corp/preview.html", channel.source.path)
        assertEquals("ccode=olivertest012&pageid=2026Junsub&to=all", channel.source.query)
    }

    @Test
    fun routeDynamicCampaignDomainCorpPreviewAsMarketingChannel() {
        val event = WvUrlEvent.fromUri(
            Uri.parse("https://campaign-domain.example/m/corp/preview.html?pageid=dynamic2026&ccode=dynamic_ccode")
        )

        assertTrue(event is WvUrlEvent.Channel)
        val channel = event as WvUrlEvent.Channel
        assertEquals("/m/corp/preview.html", channel.source.path)
        assertEquals("pageid=dynamic2026&ccode=dynamic_ccode", channel.source.query)
    }

    @Test
    fun doNotTreatExternalCorpPathAsInternal() {
        val event = WvUrlEvent.fromUri(
            Uri.parse("https://example.com/m/corp/service.html")
        )

        assertTrue(event is WvUrlEvent.External)
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
