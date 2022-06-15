package com.ft.ftchinese.ui.article.audio

import com.ft.ftchinese.model.content.Teaser

object AudioTeaserStore {
    private var aiAudioTeaser: Teaser? = null

    fun save(t: Teaser) {
        aiAudioTeaser = t
    }

    fun load(): Teaser? {
        return aiAudioTeaser
    }
}
