package com.ft.ftchinese.util

import java.security.SecureRandom

val secureRandom = SecureRandom()

/**
 * Generate a secure random number and return it as hexdecimal string.
 * @param size The number of bytes to use.
 * @return A string whose length
 */
fun generateNonce(size: Int): String {
    val result = ByteArray(size)
    secureRandom.nextBytes(result)

    return result.joinToString("") {
        String.format("%02x", it)
    }
}