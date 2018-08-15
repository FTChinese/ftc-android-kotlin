package com.ft.ftchinese.models

import android.content.Context
import android.util.Log
import com.ft.ftchinese.util.ApiEndpoint
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.experimental.async
import java.io.IOException

data class Profile(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String?,
    val gender: String?,
    val familyName: String?,
    val givenName: String?,
    val phoneNumber: String?,
    val mobileNumber: String?,
    val birthdate: String?,
    val address: Address,
    val createdAt: String,
    val updated: String,
    val newsletter: Newsletter,
    val membership: Membership
) {
    /**
     * Save user's profile to shared preferences so that we do not need to hit API every time user open's profile page.
     * Note if user edited profile data, you must re-save it to shared preferences and sync it to server.
     */
    fun save(context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREFERENCE_NAME_USER, Context.MODE_PRIVATE)

        val editor = sharedPreferences.edit()
        editor.putString(PREF_KEY_PROFILE, gson.toJson(this))
                .apply()
    }

    companion object {
        private const val TAG = "profile"
        private const val PREF_KEY_PROFILE = "profile"

        /**
         * Try to load user's profile from shared preferences.
         * If returns null, you should request user's profile data from server.
         */
        fun loadFromPref(context: Context): Profile? {
            val sharedPreferences = context.getSharedPreferences(PREFERENCE_NAME_USER, Context.MODE_PRIVATE)
            val profile = sharedPreferences.getString(PREF_KEY_PROFILE, null) ?: return null

            return try { gson.fromJson<Profile>(profile, Profile::class.java) }
            catch (e: JsonSyntaxException) {
                Log.i(TAG, "Cannot parse JSON data from preferences: $e")
                null
            }

        }

        /**
         * Delete user's profile from share preferences.
         */
        fun removeFromPref(context: Context) {
            val sharedPreferences = context.getSharedPreferences(PREFERENCE_NAME_USER, Context.MODE_PRIVATE)

            val editor = sharedPreferences.edit()
            editor.remove(PREF_KEY_PROFILE).apply()
        }

        /**
         * Request a user's profile from API.
         * @param id UUID of a user
         */
        suspend fun loadFromApi(id: String): Profile? {
            val job = async {
                Fetch.get(ApiEndpoint.PROFILE, id)
            }

            return try {
                val response = job.await() ?: return null
                val body = response.body()?.string()

                gson.fromJson<Profile>(body, Profile::class.java)

            } catch (e: IOException) {
                Log.w(TAG, "Response error: $e")
                null
            } catch (e: JsonSyntaxException) {
                Log.w(TAG, "JSON parse error: $e")
                null
            } catch (e: ErrorResponse) {
                throw e
            }
        }
    }
}

data class Address(
    val province: String,
    val city: String,
    val district: String,
    val street: String,
    val zipCode: String
)

data class Newsletter(
    val todayFocus: Boolean,
    val weeklyChoice: Boolean,
    val afternoonExpress: Boolean
)