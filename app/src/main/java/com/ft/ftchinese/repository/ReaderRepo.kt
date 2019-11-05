package com.ft.ftchinese.repository

import com.beust.klaxon.Klaxon
import com.ft.ftchinese.model.reader.ReadingDuration
import com.ft.ftchinese.util.Fetch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class ReaderRepo : AnkoLogger {

    fun engaged(dur: ReadingDuration) {
        info("Engagement length of ${dur.userId}: ${dur.startUnix} - ${dur.endUnix}")

        Fetch().post("http://www.ftchinese.com/engagment.php")
                .jsonBody(Klaxon().toJsonString(dur))
                .responseString()
    }

    companion object {
        private var instance: ReaderRepo? = null

        @Synchronized
        fun getInstance(): ReaderRepo {
            if (instance == null) {
                instance = ReaderRepo()
            }

            return instance!!
        }
    }
}
