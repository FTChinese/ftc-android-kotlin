package com.ft.ftchinese.ui.web

import com.ft.ftchinese.model.content.Language
import com.ft.ftchinese.model.enums.ArticleType
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
    fun parseLanguageSuffixWithoutLosingContentId() {
        val id = "2b2a9375-829c-4b4a-af98-c5e1e963c493"
        val teaser = teaserFromUrl("https://ai.chineseft.net/content/$id/ce?tier=premium")

        assertEquals(ArticleType.Content, teaser.type)
        assertEquals(id, teaser.id)
        assertEquals(Language.BILINGUAL, teaser.langVariant)
    }
}
