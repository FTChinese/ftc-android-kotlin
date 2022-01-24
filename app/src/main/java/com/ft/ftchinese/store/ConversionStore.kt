package com.ft.ftchinese.store

import android.content.Context
import androidx.core.content.edit
import com.ft.ftchinese.service.KEY_DUR_END

object ConversionStore {
    private const val FILE_NAME = "com.ft.ftchinese.conversion"
    private const val KEY_CAMPAIGN_ID = "campaign_id"

    fun save(ctx: Context, id: Long) {
        ctx.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).edit(commit = true) {
            putLong(KEY_CAMPAIGN_ID, id)
        }
    }

    fun exists(ctx: Context): Boolean {
        val id = ctx.getSharedPreferences(
            FILE_NAME,
            Context.MODE_PRIVATE
        )
            .getLong(KEY_DUR_END, 0)

        return id > 0
    }
}
