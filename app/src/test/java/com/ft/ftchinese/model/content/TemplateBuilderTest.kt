package com.ft.ftchinese.model.content

import com.ft.ftchinese.model.fetch.marshaller
import kotlinx.serialization.decodeFromString
import org.junit.Assert.*
import org.junit.Test

class TemplateBuilderTest {
    @Test
    fun testReplace() {
        var template = """
var follows = {
    'tag': ['{follow-tags}'],
    'topic': ['{follow-topics}'],
    'industry': ['{follow-industries}'],
    'area': ['{follow-areas}'],
    'author': ['{follow-authors}'],
    'column': ['{follow-columns}']
}""".trimIndent()

        val follows: Map<String, String> = mapOf(
            "'{follow-tags}'" to "'百度', '中美关系'",
            "'{follow-topics}'" to "'Hello'",
            "'{follow-areas}'" to "",
            "'{follow-industries}'" to "",
            "'{follow-authors}'" to "",
            "'{follow-columns}'" to ""
        )

        follows.forEach {(key ,value) ->
            template = template.replace(key, value)
        }

        println(template)
    }

    @Test
    fun replaceEmptyString() {
        val template = """{{ad-pollyfill-js}}"""

        println(template.replace("{{ad-pollyfill-js}}", ""))
    }

    @Test
    fun renderAiTranslationDisclaimer() {
        val html = aiTranslationDisclaimerHtml(
            story = storyWithTag("AITranslation,高端专享"),
            language = Language.CHINESE,
            isTraditionalCn = false
        )

        assertTrue(html.contains("ai-disclaimer-container"))
        assertTrue(html.contains("中文内容为AI翻译"))
    }

    @Test
    fun renderTraditionalAiTranslationDisclaimer() {
        val html = aiTranslationDisclaimerHtml(
            story = storyWithTag("AITranslation,高端专享"),
            language = Language.CHINESE,
            isTraditionalCn = true
        )

        assertTrue(html.contains("ai-disclaimer-container"))
        assertTrue(html.contains("中文內容為AI翻譯"))
    }

    @Test
    fun hideAiTranslationDisclaimerForEnglishView() {
        val html = aiTranslationDisclaimerHtml(
            story = storyWithTag("AITranslation,高端专享"),
            language = Language.ENGLISH,
            isTraditionalCn = false
        )

        assertEquals("", html)
    }

    @Test
    fun hideAiTranslationDisclaimerWithoutTag() {
        val html = aiTranslationDisclaimerHtml(
            story = storyWithTag("高端专享"),
            language = Language.CHINESE,
            isTraditionalCn = false
        )

        assertEquals("", html)
    }

    private fun storyWithTag(tag: String): Story {
        val data = """
            {
              "id": "2b2a9375-829c-4b4a-af98-c5e1e963c493",
              "ftid": "2b2a9375-829c-4b4a-af98-c5e1e963c493",
              "fileupdatetime": "1778419399",
              "cheadline": "测试标题",
              "clongleadbody": "测试导语",
              "cbody": "<p>测试正文</p>",
              "eheadline": "Test headline",
              "elongleadbody": "Test standfirst",
              "ebody": "<p>Test body</p>",
              "cbyline_description": "FT中文网",
              "cbyline_status": "",
              "tag": "$tag",
              "genre": "",
              "topic": "",
              "industry": "",
              "area": "",
              "last_publish_time": "1778419399",
              "story_pic": {}
            }
        """.trimIndent()

        return marshaller.decodeFromString(data)
    }
}
