package com.ft.ftchinese.model

enum class RemoteMessageType(val symbol: String) {
    Story("story"),
    Video("video"),
    Photo("photo"),
    Academy("gym"),
    SpecialReport("special"),
    Tag("tag"),
    Channel("channel"),
    Other("page"),
    Download("download");

    override fun toString(): String {
        return symbol
    }

    companion object {
        private val stringToEnum: Map<String, RemoteMessageType> = values().associateBy { it.symbol }

        @JvmStatic
        fun fromString(symbol: String?): RemoteMessageType? {
            return stringToEnum[symbol]
        }
    }
}
