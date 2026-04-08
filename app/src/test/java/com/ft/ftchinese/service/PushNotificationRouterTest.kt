package com.ft.ftchinese.service

import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.model.enums.ArticleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PushNotificationRouterTest {

    @Test
    fun routesPremiumActionToPremiumArticle() {
        val route = PushNotificationRouter.routeFromData(
            data = mapOf(
                "action" to "premium",
                "id" to "001084989",
                "title" to "Premium Story"
            )
        )

        assertTrue(route is PushRoute.Article)
        val teaser = (route as PushRoute.Article).teaser
        assertEquals(ArticleType.Premium, teaser.type)
        assertEquals("001084989", teaser.id)
    }

    @Test
    fun routesPageActionToWebpageWhenSubtypeIsNotSpecial() {
        val route = PushNotificationRouter.routeFromData(
            data = mapOf(
                "action" to "page",
                "id" to "https://www.ftchinese.com/m/corp/preview.html?pageid=events",
                "title" to "Events"
            )
        )

        assertTrue(route is PushRoute.Web)
        val page = (route as PushRoute.Web).meta
        assertEquals("https://www.ftchinese.com/m/corp/preview.html?pageid=events", page.url)
    }

    @Test
    fun routesRadioActionToInteractiveRadioTeaser() {
        val route = PushNotificationRouter.routeFromData(
            data = mapOf(
                "action" to "radio",
                "id" to "12345",
                "audio" to "https://example.com/audio.mp3",
                "title" to "Radio"
            )
        )

        assertTrue(route is PushRoute.Article)
        val teaser = (route as PushRoute.Article).teaser
        assertEquals(ArticleType.Interactive, teaser.type)
        assertEquals(Teaser.SUB_TYPE_RADIO, teaser.subType)
        assertEquals("https://example.com/audio.mp3", teaser.radioUrl)
    }

    @Test
    fun routesPageBilingualPayloadToInteractiveBilingualTeaser() {
        val route = PushNotificationRouter.routeFromData(
            data = mapOf(
                "action" to "page",
                "id" to "https://www.ftchinese.com/interactive/12345",
                "subtype" to "bilingual",
                "title" to "Bilingual"
            )
        )

        assertTrue(route is PushRoute.Article)
        val teaser = (route as PushRoute.Article).teaser
        assertEquals(ArticleType.Interactive, teaser.type)
        assertEquals("bilingual", teaser.subType)
        assertEquals("12345", teaser.id)
    }

    @Test
    fun routesPageBilingualUuidPayloadToInteractiveBilingualTeaser() {
        val route = PushNotificationRouter.routeFromData(
            data = mapOf(
                "action" to "page",
                "id" to "https://www.chineseft.net/interactive/e49f0110-f07b-4480-9ea4-2b37aab2c906",
                "subtype" to "bilingual",
                "title" to "Bilingual UUID"
            )
        )

        assertTrue(route is PushRoute.Article)
        val teaser = (route as PushRoute.Article).teaser
        assertEquals(ArticleType.Interactive, teaser.type)
        assertEquals("bilingual", teaser.subType)
        assertEquals("e49f0110-f07b-4480-9ea4-2b37aab2c906", teaser.id)
    }

    @Test
    fun routesInteractiveBilingualUuidPayloadToInteractiveBilingualTeaser() {
        val route = PushNotificationRouter.routeFromData(
            data = mapOf(
                "action" to "interactive",
                "id" to "https://www.chineseft.net/interactive/e49f0110-f07b-4480-9ea4-2b37aab2c906",
                "subtype" to "bilingual",
                "title" to "Bilingual UUID Interactive"
            )
        )

        assertTrue(route is PushRoute.Article)
        val teaser = (route as PushRoute.Article).teaser
        assertEquals(ArticleType.Interactive, teaser.type)
        assertEquals("bilingual", teaser.subType)
        assertEquals("e49f0110-f07b-4480-9ea4-2b37aab2c906", teaser.id)
    }

    @Test
    fun routesGymSpeedreadingPayloadToInteractiveSpeedreadingTeaser() {
        val route = PushNotificationRouter.routeFromData(
            data = mapOf(
                "action" to "gym",
                "id" to "54321",
                "subtype" to "speedreading",
                "title" to "Speed Reading"
            )
        )

        assertTrue(route is PushRoute.Article)
        val teaser = (route as PushRoute.Article).teaser
        assertEquals(ArticleType.Interactive, teaser.type)
        assertEquals(Teaser.SUB_TYPE_SPEED_READING, teaser.subType)
        assertEquals("54321", teaser.id)
    }

    @Test
    fun returnsNullForBlankContentId() {
        val route = PushNotificationRouter.routeFromData(
            data = mapOf(
                "action" to "story",
                "id" to "   ",
                "title" to "Broken Payload"
            )
        )

        assertNull(route)
    }

    @Test
    fun returnsNullForUnsupportedAction() {
        val route = PushNotificationRouter.routeFromData(
            data = mapOf(
                "action" to "message",
                "id" to "12345",
                "title" to "Unsupported"
            )
        )

        assertNull(route)
    }
}
