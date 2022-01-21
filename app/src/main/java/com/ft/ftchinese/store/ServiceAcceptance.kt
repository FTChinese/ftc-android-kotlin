package com.ft.ftchinese.store

import android.content.Context
import androidx.core.content.edit

private const val SESSION_PREF_NAME = "ftc_service_acceptance"
private const val PREF_IS_ACCEPTED = "is_accepted"

class ServiceAcceptance private constructor(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(SESSION_PREF_NAME, Context.MODE_PRIVATE)

    fun isAccepted(): Boolean {
        return sharedPreferences.getBoolean(PREF_IS_ACCEPTED, false)
    }

    fun accepted() {
        sharedPreferences.edit {
            putBoolean(PREF_IS_ACCEPTED, true)
        }
    }

    fun clear() {
        sharedPreferences.edit {
            clear()
        }
    }

    companion object {
        private var instance: ServiceAcceptance? = null

        @Synchronized fun getInstance(ctx: Context): ServiceAcceptance {
            if (instance == null) {
                instance = ServiceAcceptance(ctx.applicationContext)
            }

            return instance!!
        }
    }
}
