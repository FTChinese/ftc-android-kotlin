package com.ft.ftchinese.models

import android.content.Context
import android.util.Log
import com.ft.ftchinese.util.gson
import com.google.gson.JsonSyntaxException

const val PREFERENCE_NAME_USER = "user"

data class User(
        val id: String,
        val name: String?,
        val email: String,
        val avatar: String?,
        val isVip: Boolean,
        val verified: Boolean,
        val membership: Membership
) {
    fun save(context: Context?) {
        val sharedPreferences = context?.getSharedPreferences(PREFERENCE_NAME_USER, Context.MODE_PRIVATE)

        val editor = sharedPreferences?.edit()
        editor?.putString(PREF_KEY_COOKIE, gson.toJson(this))
                ?.apply()
    }

    companion object {
        private const val PREF_KEY_COOKIE = "cookie"
        private const val TAG = "User"

        fun loadFromPref(context: Context): User? {
            val sharedPreferences = context.getSharedPreferences(PREFERENCE_NAME_USER, Context.MODE_PRIVATE)
            val cookie = sharedPreferences.getString(PREF_KEY_COOKIE, null) ?: return null

            return try { gson.fromJson<User>(cookie, User::class.java) }
            catch (e: JsonSyntaxException) {
                Log.i(TAG, e.toString())

                null
            }
        }

        fun removeFromPref(context: Context) {
            val sharedPreferences = context.getSharedPreferences(PREFERENCE_NAME_USER, Context.MODE_PRIVATE)

            val editor = sharedPreferences.edit()
            editor.remove(PREF_KEY_COOKIE).apply()
        }
    }
}

