package com.ft.ftchinese.model.content

import com.ft.ftchinese.model.enums.Tier

/**
 * Build JS string and put into html file by string replacement.
 */
class JsBuilder {
    private var snippets: MutableList<String> = mutableListOf()

    /**
     * Used in an article page to change font size.
     */
    fun withFontSize(size: String): JsBuilder {
        snippets.add("""
            if (typeof checkFontSize === 'function') {
                checkFontSize('$size');
            }
        """.trimIndent())
        return this
    }

    /**
     * Used in the search page.
     */
    fun withSearch(keyword: String): JsBuilder {
        snippets.add("""
            search('$keyword');
        """.trimIndent())
        return this
    }

    /**
     * Used in a channel page to enable locker icon.
     */
    fun withLockerIcon(tier: Tier?): JsBuilder {
        val prvl = when (tier) {
            Tier.STANDARD -> """['premium']"""
            Tier.PREMIUM -> """['premium', 'EditorChoice']"""
            else -> "[]"
        }

        snippets.add(JsSnippets.lockerIcon(tier))
        return this
    }

    fun build(): String {
        return """
        <script>${snippets.joinToString("\n")}</script>
        """.trimIndent()
    }

    fun appendToHtml(htmlStr: String): String {
        val js = build() + "</html>"
        return htmlStr.replace("</html>", js)
    }
}
