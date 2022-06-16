package com.ft.ftchinese.ui.article

enum class ArticleAppScreen(
    val title: String,
) {
    Channel(""),
    Story(""),
    Screenshot("分享截图"),
    Audio("");

    companion object {
        @JvmStatic
        fun fromRoute(route: String?): ArticleAppScreen =
            when (route?.substringBefore("/")) {
                Channel.name -> Channel
                Story.name -> Story
                Screenshot.name -> Screenshot
                Audio.name -> Audio
                null -> Story
                else -> throw IllegalArgumentException("Route $route is not recognized")
            }
    }
}
