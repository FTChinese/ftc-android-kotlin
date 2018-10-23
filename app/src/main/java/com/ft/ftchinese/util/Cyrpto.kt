package com.ft.ftchinese.util

import java.security.SecureRandom

val secureRandom = SecureRandom()

fun generateNonce(size: Int): String {
    val result = ByteArray(size)
    secureRandom.nextBytes(result)

    return result.joinToString("") {
        String.format("%02x", it)
    }
}