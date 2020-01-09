package com.ft.ftchinese.model.apicontent

data class Authors (
        val names: List<String>,
        val place: String
)

data class Byline(
        val organization: String,
        val authors: List<Authors>
)
