package com.ft.ftchinese.database

import android.database.Cursor
import android.database.CursorWrapper
import com.ft.ftchinese.models.ChannelItem

class ArticleCursorWrapper(cursor: Cursor) : CursorWrapper(cursor) {
    fun loadItem(): ChannelItem {
        val articleId = getString(getColumnIndex(HistoryTable.Cols.ID))
        val articleType = getString(getColumnIndex(HistoryTable.Cols.TYPE))
        val title = getString(getColumnIndex(HistoryTable.Cols.TITLE))
        val standfirst = getString(getColumnIndex(HistoryTable.Cols.STANDFIRST))

        val item = ChannelItem(id = articleId, type = articleType, headline = title)
        item.standfirst = standfirst

        return item
    }
}