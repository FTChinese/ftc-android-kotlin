package com.ft.ftchinese.ui.web

import com.ft.ftchinese.model.content.Language
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.model.enums.ArticleType
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeaserNavigationGuardTest {
    @After
    fun tearDown() {
        TeaserNavigationGuard.reset()
    }

    @Test
    fun rejectSameTeaserWithinDuplicateWindow() {
        val teaser = teaser("2b2a9375-829c-4b4a-af98-c5e1e963c493")

        assertTrue(TeaserNavigationGuard.accept(teaser, nowMs = 10_000))
        assertFalse(TeaserNavigationGuard.accept(teaser, nowMs = 10_500))
    }

    @Test
    fun allowSameTeaserAfterDuplicateWindow() {
        val teaser = teaser("2b2a9375-829c-4b4a-af98-c5e1e963c493")

        assertTrue(TeaserNavigationGuard.accept(teaser, nowMs = 10_000))
        assertTrue(TeaserNavigationGuard.accept(teaser, nowMs = 11_300))
    }

    @Test
    fun rejectDifferentTeaserWithinSameTapWindow() {
        assertTrue(TeaserNavigationGuard.accept(teaser("content-a"), nowMs = 10_000))
        assertFalse(TeaserNavigationGuard.accept(teaser("content-b"), nowMs = 10_500))
    }

    @Test
    fun allowDifferentTeaserAfterSameTapWindow() {
        assertTrue(TeaserNavigationGuard.accept(teaser("content-a"), nowMs = 10_000))
        assertTrue(TeaserNavigationGuard.accept(teaser("content-b"), nowMs = 10_800))
    }

    @Test
    fun includeSubTypeAndLanguageInNavigationKey() {
        assertTrue(
            TeaserNavigationGuard.accept(
                teaser(
                    id = "230079",
                    type = ArticleType.Interactive,
                    subType = "bilingual",
                    language = Language.CHINESE
                ),
                nowMs = 10_000
            )
        )
        assertTrue(
            TeaserNavigationGuard.accept(
                teaser(
                    id = "230079",
                    type = ArticleType.Interactive,
                    subType = "bilingual",
                    language = Language.BILINGUAL
                ),
                nowMs = 10_800
            )
        )
    }

    @Test
    fun activityStartGuardIsIndependentFromInteractionGuard() {
        val teaser = teaser("2b2a9375-829c-4b4a-af98-c5e1e963c493")

        assertTrue(TeaserNavigationGuard.accept(teaser, nowMs = 10_000))
        assertTrue(TeaserNavigationGuard.acceptActivityStart(teaser, nowMs = 10_000))
        assertFalse(TeaserNavigationGuard.acceptActivityStart(teaser("content-b"), nowMs = 10_500))
    }

    private fun teaser(
        id: String,
        type: ArticleType = ArticleType.Content,
        subType: String? = null,
        language: Language = Language.CHINESE
    ): Teaser {
        return Teaser(
            id = id,
            type = type,
            subType = subType,
            title = "",
            langVariant = language
        )
    }
}
