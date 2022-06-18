package com.ft.ftchinese.store

import android.content.Context
import androidx.core.content.edit
import com.ft.ftchinese.service.KEY_DUR_END

private const val FILE_NAME = "com.ft.ftchinese.conversion"
private const val KEY_CAMPAIGN_ID = "campaign_id"

class ConversionStore private constructor(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(
        FILE_NAME,
        Context.MODE_PRIVATE
    )

    fun save(id: Long) {
        sharedPreferences.edit(commit = true) {
            putLong(KEY_CAMPAIGN_ID, id)
        }
    }

    fun exists(): Boolean {
        val id = sharedPreferences.getLong(KEY_DUR_END, 0)

        return id > 0
    }

    companion object {
        private var instance: ConversionStore? = null

        @Synchronized
        fun getInstance(context: Context): ConversionStore {
            if (instance == null) {
                instance = ConversionStore(context.applicationContext)
            }

            return instance!!
        }
    }
}

