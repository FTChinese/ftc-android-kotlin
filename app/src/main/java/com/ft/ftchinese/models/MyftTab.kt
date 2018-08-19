package com.ft.ftchinese.models

data class MyftTab(
        val id: Int,
        val title: String
) {
    companion object {
        const val READING_HISTORY = 0x01
        const val STARRED_ARTICLE = 0x02
        const val FOLLOWING = 0x03

        val pages = arrayOf(
                MyftTab(id = READING_HISTORY, title = "阅读历史"),
                MyftTab(id = STARRED_ARTICLE, title = "收藏文章"),
                MyftTab(id = FOLLOWING, title = "关注")
        )
    }
}