package com.ft.ftchinese.database

import android.database.Cursor
import android.database.CursorWrapper
import com.ft.ftchinese.models.ChannelItem

/**
 * Used to wrap query result.
 */
class ArticleCursorWrapper(cursor: Cursor) : CursorWrapper(cursor) {
    fun loadItem(): ChannelItem {
        val articleId = getString(getColumnIndex(Cols.ID))
        val articleType = getString(getColumnIndex(Cols.TYPE))
        val subType = getString(getColumnIndex(Cols.SUB_TYPE))
        val title = getString(getColumnIndex(Cols.TITLE))
        val standfirst = getString(getColumnIndex(Cols.STANDFIRST))
        val audioUrl = getString(getColumnIndex(Cols.AUDIO_URL))
        val radioUrl = getString(getColumnIndex(Cols.RADIO_URL))
        val publishedAt = getString(getColumnIndex(Cols.PUBLISHED_AT))

        val item = ChannelItem(
                id = articleId,
                type = articleType,
                subType = subType,
                headline = title,
                eaudio = audioUrl,
                shortlead = radioUrl,
                timeStamp = publishedAt
        )

        item.standfirst = standfirst

        return item
    }
}