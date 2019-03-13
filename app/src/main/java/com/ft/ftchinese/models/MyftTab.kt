package com.ft.ftchinese.models

data class MyftTab(
        val id: MyftTabId,
        val title: String
)

enum class MyftTabId {
    READ,
    STARRED,
    FOLLOWING
}