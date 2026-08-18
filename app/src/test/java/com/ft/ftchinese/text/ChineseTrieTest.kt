package com.ft.ftchinese.text

import org.junit.Assert.assertEquals
import org.junit.Test

class ChineseTrieTest {
    private val trie = ChineseTrie.fromDictionary(
        linkedMapOf(
            "中国" to "中國",
            "中国人" to "中國人",
            "支持" to "支援",
            "台积电" to "台積電",
        )
    )

    @Test
    fun usesLongestMatch() {
        assertEquals("中國人支持台積電", trie.convert("中国人支持台积电"))
    }

    @Test
    fun preservesNonDictionaryTextAndNormalizesMiddleDot() {
        assertEquals("ABC•123", trie.convert("ABC·123"))
    }

    @Test
    fun handlesSupplementaryCharacters() {
        assertEquals("😀中國", trie.convert("😀中国"))
    }
}
