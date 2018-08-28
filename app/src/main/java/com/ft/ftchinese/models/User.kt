package com.ft.ftchinese.models

import android.content.Context
import android.util.Log
import com.ft.ftchinese.util.gson
import com.google.gson.JsonSyntaxException

const val PREFERENCE_NAME_USER = "user"

/**
 * A user's essential data.
 * All fields should be declared as `var` except `id` which should never be changed.
 * When user changes data like email, user name, verified email, purchased subscription, the corresponding fields should be updated and saved to shared preferences.
 */
data class User(
        val id: String,
        var name: String,
        var email: String,
        var avatar: String,
        var isVip: Boolean = false,
        var verified: Boolean = false,
        var membership: Membership
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

        fun loadFromPref(context: Context?): User? {
            if (context == null) {
                return null
            }
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

