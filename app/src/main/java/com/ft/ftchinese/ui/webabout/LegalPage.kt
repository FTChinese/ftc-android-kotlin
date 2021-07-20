package com.ft.ftchinese.ui.webabout

data class LegalPage (
    val title: String,
    val url: String,
)

val legalPages = listOf(
    LegalPage(
        title = "服务条款",
        url = "http://www.ftacademy.cn/service.html",
    ),
    LegalPage(
        title = "隐私政策",
        url = "http://www.ftacademy.cn/privacy.html",
    )
)
