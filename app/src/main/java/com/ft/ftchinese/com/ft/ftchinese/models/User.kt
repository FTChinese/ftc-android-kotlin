package com.ft.ftchinese.com.ft.ftchinese.models

import android.content.Context
import android.icu.text.IDNA
import android.util.Log
import com.ft.ftchinese.gson
import com.google.gson.JsonSyntaxException
import org.jetbrains.anko.AnkoLogger

data class User(
        val id: String,
        val name: String?,
        val email: String,
        val avatar: String?,
        val isVip: Boolean,
        val verified: Boolean,
        val membership: Membership
) {
    fun save(context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

        val editor = sharedPreferences.edit()
        editor.putString("cookie", gson.toJson(this))
                .apply()
    }

    companion object {
        private const val PREFERENCE_NAME = "user"
        private const val TAG = "user"

        fun loadFromPref(context: Context): User? {
            val sharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            val cookie = sharedPreferences.getString("cookie", null) ?: return null

            return try { gson.fromJson<User>(cookie, User::class.java) }
            catch (e: JsonSyntaxException) {
                Log.i(TAG, e.toString())

                null
            }
        }
    }
}

data class Membership(
        val type: String,
        val startAt: String?,
        val expireAt: String?
)