package com.ft.ftchinese.database

import android.provider.BaseColumns

object Cols : BaseColumns {
    const val ID = "id"
    const val TYPE = "type"
    const val SUB_TYPE = "sub_type"
    const val TITLE = "title"
    const val STANDFIRST = "standfirst"
    const val AUDIO_URL = "audio_url"
    const val RADIO_URL = "radio_url"
    const val PUBLISHED_AT = "published_at"
    const val READ_AT = "read_at"

    val stmtCreate = """
        ${BaseColumns._ID} INTEGER PRIMARY KEY,
        ${Cols.ID} TEXT,
        ${Cols.TYPE} TEXT,
        ${Cols.SUB_TYPE} TEXT,
        ${Cols.TITLE} TEXT,
        ${Cols.STANDFIRST} TEXT,
        ${Cols.AUDIO_URL} TEXT,
        ${Cols.RADIO_URL} TEXT,
        ${Cols.PUBLISHED_AT} TEXT,
        ${Cols.READ_AT} TEXT DEFAULT CURRENT_TIMESTAMP,
        UNIQUE (${Cols.ID}, ${Cols.TYPE}) ON CONFLICT IGNORE
    """.trimIndent()
}

object StarredTable {
    const val NAME = "starred_article"

    val create = """
        CREATE TABLE IF NOT EXISTS $NAME (
        ${Cols.stmtCreate})
    """.trimIndent()

    const val drop = "DROP TABLE IF EXISTS $NAME"
}

object HistoryTable {
    const val NAME = "reading_history"

    val create = """
        CREATE TABLE IF NOT EXISTS $NAME (
        ${Cols.stmtCreate})
    """.trimIndent()

    const val drop = "DROP TABLE IF EXISTS $NAME"
}