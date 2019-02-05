package com.ft.ftchinese.splash

import com.ft.ftchinese.util.KDate
import com.ft.ftchinese.util.json
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.threeten.bp.LocalDate
import java.io.File

data class ScheduleToday(
        @KDate
        val date: LocalDate,
        val items: List<ScreenAd>
) : AnkoLogger {
    fun cache(file: File) {
        try {
            file.writeText(json.toJsonString(this))
        } catch (e: Exception) {
            info(e.message)
        }
    }
}