package com.ft.ftchinese

import org.junit.Test
import java.security.SecureRandom

class RandomTest {
    @Test fun mathRandom() {
        println(Math.random())
    }

    @Test fun secureRandom() {
        val random = SecureRandom()
        val result = ByteArray(4)
        random.nextBytes(result)

//        result.map {
//            String.format("%02x", it)
//        }.joinToString()

        val v = result.joinToString("") {
            String.format("%02x", it)
        }

        println(v)
    }
}