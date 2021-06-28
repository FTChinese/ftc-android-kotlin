package com.ft.ftchinese.ui.article

data class BookmarkState(
    val isStarring: Boolean,
    // Used in snackbar when user clicked star icon.
    // Upon initial loading, the state is retrieved from DB
    // and you should not show any message.
    val message: Int? = null,
)
