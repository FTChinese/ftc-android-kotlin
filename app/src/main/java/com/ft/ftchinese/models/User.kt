package com.ft.ftchinese.models

import android.content.Context
import android.util.Log
import com.ft.ftchinese.util.ApiEndpoint
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.experimental.async
import java.io.IOException

const val PREFERENCE_NAME_USER = "user"

/**
 * A user's essential data.
 * All fields should be declared as `var` except `id` which should never be changed.
 * When user changes data like email, user name, verified email, purchased subscription, the corresponding fields should be updated and saved to shared preferences.
 * Avoid modifying an instance when user's data changed so that everything is immutable.
 */
data class User(
        val id: String,
        val name: String,
        val email: String,
        val avatar: String,
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

    /**
     * @return User. Always returns a new one rather than modifying the existing one to make it immutable.
     * @throws ErrorResponse If HTTP response status is above 400.
     * @throws IllegalStateException If request url is empty.
     * @throws IOException If network request failed, or response body can not be read, regardless of if response is successful or not.
     * @throws JsonSyntaxException If the content returned by API could not be parsed into valid JSON, regardless of if response is successful or not
     */
    suspend fun refresh(): User {
        val job = async {
            Fetch().get(ApiEndpoint.ACCOUNT)
                    .noCache()
                    .setUserId(this@User.id)
                    .end()
        }

        val response = job.await()

        val body = response.body()?.string()

        return gson.fromJson<User>(body, User::class.java)
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

