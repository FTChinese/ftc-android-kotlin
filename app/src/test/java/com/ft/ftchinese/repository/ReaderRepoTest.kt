package com.ft.ftchinese.repository

import com.ft.ftchinese.model.reader.ReadingDuration
import org.junit.Test
import java.util.*

class ReaderRepoTest {
    @Test fun readingDuration() {
        val dur = ReadingDuration(
                url = "http://www.ftchinese.com/",
                refer = "http://www.ftchinese.com/",
                startUnix = Date().time / 1000,
                endUnix = Date().time / 1000,
                userId = "00f8fb6b-ec7b-45b7-91f7-cb379df6a3a1",
                functionName = "onLoad"
        )

        val result = ReaderRepo().engaged(dur)

        println("Result: $result")
    }

    @Test fun emailExists() {
        val ok = ReaderRepo().emailExists("neefrankie@outlook.com")

        println("Email exists: $ok")
    }
}
