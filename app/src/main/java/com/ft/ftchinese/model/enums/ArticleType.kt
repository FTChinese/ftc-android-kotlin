package com.ft.ftchinese.model.enums

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
enum class ArticleType(val symbol: String) : Parcelable {
    @SerialName("story")
    Story("story"),
    @SerialName("premium")
    Premium("premium"),
    @SerialName("video")
    Video("video"),
    @SerialName("photonews")
    Gallery("photonews"),
    @SerialName("interactive")
    Interactive("interactive"),
    @SerialName("column")
    Column("column");

    override fun toString(): String {
        return symbol
    }

    companion object {

        @JvmStatic
        fun fromString(symbol: String?): ArticleType {
            return when (symbol) {
                "premium" -> Premium
                "video" -> Video
                "photonews" -> Gallery
                "interactive" -> Interactive
                "column" -> Column
                else -> Story
            }
        }
    }
}
