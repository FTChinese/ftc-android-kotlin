package com.ft.ftchinese

import com.ft.ftchinese.models.ErrorDetail
import org.junit.Test

class TestApiResponse {
    @Test fun msgKey() {
        val ed = ErrorDetail("email", "already_exists")

        println(ed.msgKey)
    }
}