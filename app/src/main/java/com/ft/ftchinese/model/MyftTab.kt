package com.ft.ftchinese.model

data class MyftTab(
        val id: MyftTabId,
        val title: String
)

enum class MyftTabId {
    READ,
    STARRED,
    FOLLOWING
}
