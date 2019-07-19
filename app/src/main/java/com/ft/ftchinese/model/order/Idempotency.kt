package com.ft.ftchinese.model.order

import android.content.Context
import androidx.core.content.edit
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.util.*

private const val PREF_FILE_NAME = "stripe_idempotency"
private const val PREF_KEY = "key"
private const val PREF_CREATED = "created"

class Idempotency private constructor(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)

    // Swipe out everything.
    fun clear() {
        sharedPreferences.edit {
            clear()
        }
    }

    fun retrieveKey(): String {
        val key = sharedPreferences.getString(PREF_KEY, null)
        val created = sharedPreferences.getString(PREF_CREATED, null)

        if (key == null || created == null) {
            return generateKey()
        }

        if (isStale(ZonedDateTime.parse(created, DateTimeFormatter.ISO_DATE_TIME))) {
            return generateKey()
        }

        return key
    }

    private fun generateKey(): String {
        val uuid = UUID.randomUUID().toString()
        val created = ZonedDateTime.now()

        sharedPreferences.edit {
            putString(PREF_KEY, uuid)
            putString(PREF_CREATED, created.format(DateTimeFormatter.ISO_DATE_TIME))
        }

        return uuid
    }

    private fun isStale(created: ZonedDateTime): Boolean {
        return created.plusHours(24).isBefore(ZonedDateTime.now())
    }

    companion object {
        private var instance: Idempotency? = null

        @JvmStatic
        @Synchronized
        fun getInstance(ctx: Context): Idempotency {
            if (instance == null) {
                instance = Idempotency(ctx.applicationContext)
            }

            return instance!!
        }
    }
}
