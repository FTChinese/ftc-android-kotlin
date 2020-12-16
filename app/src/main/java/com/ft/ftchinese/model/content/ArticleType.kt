package com.ft.ftchinese.model.content

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class ArticleType(val symbol: String) : Parcelable {
    Story("story"),
    Premium("premium"),
    Video("video"),
    Gallery("photonews"),
    Interactive("interactive"),
    Column("column");

    override fun toString(): String {
        return symbol
    }

    companion object {
        private val stringToEnum: Map<String, ArticleType> = values().associateBy { it.symbol }

        @JvmStatic
        fun fromString(symbol: String?): ArticleType {
            return stringToEnum[symbol] ?: Story
        }
    }
}
