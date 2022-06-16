package com.ft.ftchinese.ui.article

import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.ui.article.screenshot.ScreenshotMeta
import com.ft.ftchinese.ui.util.HashUtils

/**
 * NavStore is used to store complex data for navigation.
 * The problem with compose ui navigation is that is used a url
 * system resemble to web. This means it only accepts strings.
 * The ideal usage case is that you save all the complex data
 * in database and only pass an id via the url.
 * In the receiving component, use the id to retrieve the full
 * complex data from db.
 * Here I'm using a simple in-memory mechanism to act as a db.
 * Upon saving a complex data type, generate a MD5 value from
 * one or multiple field of the type and use the hash value as
 * id so that later receiving component could retrieve it.
 */
object NavStore {
    private val channelSources: MutableMap<String, ChannelSource> = mutableMapOf()
    private val teasers: MutableMap<String, Teaser> = mutableMapOf()
    private val screenshots: MutableMap<String, ScreenshotMeta> = mutableMapOf()

    // Limit memory usage. If entries exceed, remove all.
    private const val limit = 20

    fun clearAll() {
        channelSources.clear()
        teasers.clear()
    }

    // Use md5 of path field as key.
    fun saveChannel(source: ChannelSource): String {
        val key = HashUtils.md5(source.path.ifBlank {
            source.name
        })
        if (channelSources.size > limit) {
            channelSources.clear()
        }
        channelSources[key] = source
        return key
    }

    fun getChannel(key: String): ChannelSource? {
        return channelSources[key]
    }

    fun saveTeaser(teaser: Teaser): String {
        val key = HashUtils.md5("${teaser.id}_${teaser.type}")
        if (teasers.size > limit) {
            teasers.clear()
        }

        teasers[key] = teaser
        return key
    }

    fun getTeaser(key: String): Teaser? {
        return teasers[key]
    }

    fun saveScreenshot(sm: ScreenshotMeta): String {
        val key = HashUtils.md5(sm.imageUri.toString())
        if (screenshots.size > limit) {
            screenshots.clear()
        }

        screenshots[key] = sm
        return key
    }

    fun getScreenshot(key: String): ScreenshotMeta? {
        return screenshots[key]
    }
}
