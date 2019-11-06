package com.ft.ftchinese.repository

import com.beust.klaxon.Klaxon
import com.ft.ftchinese.model.reader.ReadingDuration
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.currentFlavor
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import kotlin.Exception

class ReaderRepo : AnkoLogger {

    fun engaged(dur: ReadingDuration) {
        info("Engagement length of ${dur.userId}: ${dur.startUnix} - ${dur.endUnix}")

        try {
            Fetch().post("${currentFlavor.baseUrl}/engagment.php")
                    .jsonBody(Klaxon().toJsonString(dur))
                    .responseString()
        } catch (e: Exception) {
            info("Error when tracking reading duration $e")
        }
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
