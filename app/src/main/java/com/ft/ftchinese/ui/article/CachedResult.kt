package com.ft.ftchinese.ui.article

data class CachedResult(
        // Is the cache found?
        val found: Boolean = false,
        val exception: Exception? = null
)
