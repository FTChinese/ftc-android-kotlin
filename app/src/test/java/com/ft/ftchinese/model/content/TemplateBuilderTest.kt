package com.ft.ftchinese.model.content

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
}

