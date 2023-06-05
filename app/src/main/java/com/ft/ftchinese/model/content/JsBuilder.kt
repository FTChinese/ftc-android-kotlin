package com.ft.ftchinese.model.content

import com.ft.ftchinese.model.enums.Tier

class JsBuilder {
    private var snippets: MutableList<String> = mutableListOf()

    fun withFontSize(size: String): JsBuilder {
        snippets.add("""
            if (typeof checkFontSize === 'function') {
                checkFontSize('$size');
            }
        """.trimIndent())
        return this
    }

    fun withSearch(keyword: String): JsBuilder {
        snippets.add("""
            search('$keyword');
        """.trimIndent())
        return this
    }

    fun withLockerIcon(tier: Tier?): JsBuilder {
        val prvl = when (tier) {
            Tier.STANDARD -> """['premium']"""
            Tier.PREMIUM -> """['premium', 'EditorChoice']"""
            else -> "[]"
        }

        snippets.add("""
        (function() {
            window.gPrivileges=$prvl;
            updateHeadlineLocks();
            return window.gPrivileges;
        })()
        """.trimIndent())
        return this
    }

    fun build(): String {
        return """
        <script>${snippets.joinToString("\n")}</script>
        """.trimIndent()
    }
}
