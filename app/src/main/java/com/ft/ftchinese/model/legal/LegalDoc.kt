package com.ft.ftchinese.model.legal

data class LegalDoc(
    val title: String,
    val content: String,
)

val legalDocs = listOf(
    LegalDoc(
        title = "服务条款与隐私政策",
        content = privacyDocument
    ),
    LegalDoc(
        title = "版权声明",
        content = copyRight
    ),
)
