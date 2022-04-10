package com.ft.ftchinese.model.fetch

import kotlinx.serialization.json.Json

val marshaller = Json {
    ignoreUnknownKeys = true
}
