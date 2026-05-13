package com.ft.ftchinese.ui.web

import com.ft.ftchinese.model.content.Teaser

internal object TeaserNavigationGuard {
    private const val DUPLICATE_WINDOW_MS = 1200L
    private const val SAME_TAP_WINDOW_MS = 700L

    private val interactionGuard = RecentTeaserGuard()
    private val activityStartGuard = RecentTeaserGuard()

    fun accept(teaser: Teaser, nowMs: Long = monotonicNowMs()): Boolean {
        return interactionGuard.accept(teaser, nowMs)
    }

    fun acceptActivityStart(teaser: Teaser, nowMs: Long = monotonicNowMs()): Boolean {
        return activityStartGuard.accept(teaser, nowMs)
    }

    fun reset() {
        interactionGuard.reset()
        activityStartGuard.reset()
    }

    private class RecentTeaserGuard {
        private var lastKey: String = ""
        private var lastAtMs: Long = 0

        fun accept(teaser: Teaser, nowMs: Long): Boolean {
            val key = teaser.navigationKey()

            synchronized(this) {
                val elapsedMs = nowMs - lastAtMs
                if (
                    lastAtMs != 0L &&
                    nowMs >= lastAtMs &&
                    (
                        elapsedMs < SAME_TAP_WINDOW_MS ||
                        (key == lastKey && elapsedMs < DUPLICATE_WINDOW_MS)
                    )
                ) {
                    return false
                }

                lastKey = key
                lastAtMs = nowMs
                return true
            }
        }

        fun reset() {
            synchronized(this) {
                lastKey = ""
                lastAtMs = 0
            }
        }
    }

    private fun monotonicNowMs(): Long {
        return System.nanoTime() / 1_000_000
    }

    private fun Teaser.navigationKey(): String {
        return listOf(
            type.symbol,
            id,
            subType.orEmpty(),
            langVariant.symbol
        ).joinToString(":")
    }
}
