package com.ft.ftchinese.model.fetch

import kotlinx.serialization.json.Json

val marshaller = Json {
    ignoreUnknownKeys = true
    // This is important.
    // The default false value is not compatible with Golang's unmarshal mechanism.
    // For instance, if you have a default value `true`,
    // Kotlin will ignore it. When request sent to Go,
    // the zero value is false.
    encodeDefaults = true
}
