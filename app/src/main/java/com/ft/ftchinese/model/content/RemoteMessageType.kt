package com.ft.ftchinese.model.content


enum class RemoteMessageType(val symbol: String) {
    Story("story"),
    Video("video"),
    Photo("photo"),
    Interactive("interactive"),
    Tag("tag"),
    Channel("channel");

    override fun toString(): String {
        return symbol
    }

    fun toArticleType(): String? {
        return messageToContentType[this]
    }

    companion object {
        private val stringToEnum: Map<String, RemoteMessageType> = values().associateBy { it.symbol }

        @JvmStatic
        fun fromString(symbol: String?): RemoteMessageType? {
            return stringToEnum[symbol]
        }
    }
}

private val messageToContentType = mapOf(
        RemoteMessageType.Story to "story",
        RemoteMessageType.Video to "video",
        RemoteMessageType.Photo to "photo",
        RemoteMessageType.Interactive to "interactive"
)
