package com.ft.ftchinese.model.reader

enum class UnlinkAnchor(val symbol: String) {
    FTC("ftc"),
    WECHAT("wechat");

    override fun toString(): String {
        return symbol
    }

    companion object {
        private val stringToEnum: Map<String, UnlinkAnchor> = values().associateBy {
            it.symbol
        }

        @JvmStatic
        fun fromString(symbol: String?): UnlinkAnchor? {
            return if (symbol == null) null else stringToEnum[symbol]
        }
    }
}
