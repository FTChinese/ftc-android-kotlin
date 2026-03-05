package com.ft.ftchinese.ui.web

import android.net.Uri
import com.ft.ftchinese.model.enums.ArticleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TeaserFromUriTest {

    @Test
    fun parseInteractiveSubtypeFromQuery() {
        val teaser = teaserFromUri(
            Uri.parse("https://www.ftchinese.com/interactive/12345?subtype=bilingual&tier=premium")
        )

        assertEquals(ArticleType.Interactive, teaser.type)
        assertEquals("bilingual", teaser.subType)
        assertTrue(teaser.hasJsAPI)
    }

    @Test
    fun normalizeLegacyFtArticleSubtype() {
        val teaser = teaserFromUri(
            Uri.parse("https://www.ftchinese.com/interactive/12345?subtype=FTArticle")
        )

        assertEquals("bilingual", teaser.subType)
        assertTrue(teaser.hasJsAPI)
    }

    @Test
    fun interactiveWithoutSubtypeKeepsHtmlFallback() {
        val teaser = teaserFromUri(
            Uri.parse("https://www.ftchinese.com/interactive/12345?tier=free")
        )

        assertNull(teaser.subType)
        assertFalse(teaser.hasJsAPI)
    }

    @Test
    fun ignoreSubtypeQueryForStoryLink() {
        val teaser = teaserFromUri(
            Uri.parse("https://www.ftchinese.com/story/12345?subtype=bilingual")
        )

        assertEquals(ArticleType.Story, teaser.type)
        assertNull(teaser.subType)
        assertTrue(teaser.hasJsAPI)
    }
}
