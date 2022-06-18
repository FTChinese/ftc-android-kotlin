package com.ft.ftchinese.ui.main.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.ft.ftchinese.R

sealed class MainNavScreen(
    val route: String,
    @StringRes val titleId: Int,
    @DrawableRes val iconId: Int,
    val showBottomBar: Boolean = true,
    val showTopBar: Boolean = true,
) {
    object News : MainNavScreen(
        route = "news",
        titleId = R.string.nav_news,
        iconId = R.drawable.news_inactive
    )
    object English : MainNavScreen(
        route = "english",
        titleId = R.string.nav_english,
        iconId = R.drawable.english_inactive
    )
    object FtAcademy : MainNavScreen(
        route = "ftacademy",
        titleId = R.string.nav_ftacademy,
        iconId = R.drawable.fta_inactive
    )
    object Video : MainNavScreen(
        route = "video",
        titleId = R.string.nav_video,
        iconId = R.drawable.video_inactive
    )
    object MyFt: MainNavScreen(
        route = "myft",
        titleId = R.string.nav_myft,
        iconId = R.drawable.myft_inactive
    )
    object Search: MainNavScreen(
        route = "search",
        titleId = R.string.action_search,
        iconId = R.drawable.ic_search_black_24dp,
        showBottomBar = false,
        showTopBar = false,
    )
    object ReadArticles: MainNavScreen(
        route = "read_articles",
        titleId = R.string.myft_reading_history,
        iconId = R.drawable.ic_baseline_article_24,
        showBottomBar = false,
        showTopBar = true,
    )
    object StarredArticles : MainNavScreen(
        route = "starred_articles",
        titleId = R.string.myft_starred_articles,
        iconId = R.drawable.ic_baseline_article_24,
        showBottomBar = false,
        showTopBar = true,
    )
    object FollowedTopics : MainNavScreen(
        route = "followed_topics",
        titleId = R.string.myft_following,
        iconId = R.drawable.ic_baseline_topic_24,
        showBottomBar = false,
        showTopBar = true
    )

    companion object {
        @JvmStatic
        fun fromRoute(route: String?): MainNavScreen =
            when (route?.substringBefore("/")) {
                News.route -> News
                English.route -> English
                FtAcademy.route -> FtAcademy
                Video.route -> Video
                MyFt.route -> MyFt
                Search.route -> Search
                ReadArticles.route -> ReadArticles
                StarredArticles.route -> StarredArticles
                FollowedTopics.route -> FollowedTopics
                null -> News
                else -> throw IllegalArgumentException("Route $route is not recognized")
            }
    }
}
