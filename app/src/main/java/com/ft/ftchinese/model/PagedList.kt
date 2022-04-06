package com.ft.ftchinese.model

data class PagedList<T>(
    val total: Int,
    val page: Int,
    val limit: Int,
    val data: List<T>,
)
