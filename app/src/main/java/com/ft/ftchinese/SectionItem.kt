package com.ft.ftchinese

data class SectionItem(
        val id: String,
        val type: String,
        val createdAt: String
)

fun getApiUrl(articleType: String): String? {
    return when(articleType) {
        "story", "premium" -> "https://api.ftmailbox.com/index.php/jsapi/get_story_more_info/"
        "interactive" -> ""
        "video" -> ""
        else -> null
    }
}