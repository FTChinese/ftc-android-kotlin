package com.ft.ftchinese.models

import android.content.Context
import android.util.Log
import com.ft.ftchinese.util.gson
import com.google.gson.JsonSyntaxException

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
     * Save user's profile to shared preferences_profile so that we do not need to hit API every time user open's profile page.
     * Note if user edited profile data, you must re-save it to shared preferences_profile and sync it to server.
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
         * Try to load user's profile from shared preferences_profile.
         * If returns null, you should request user's profile data from server.
         */
        fun loadFromPref(context: Context): Profile? {
            val sharedPreferences = context.getSharedPreferences(PREFERENCE_NAME_USER, Context.MODE_PRIVATE)
            val profile = sharedPreferences.getString(PREF_KEY_PROFILE, null) ?: return null

            return try { gson.fromJson<Profile>(profile, Profile::class.java) }
            catch (e: JsonSyntaxException) {
                Log.i(TAG, "Cannot parse JSON data from preferences_profile: $e")
                null
            }

        }

        /**
         * Delete user's profile from share preferences_profile.
         */
        fun removeFromPref(context: Context) {
            val sharedPreferences = context.getSharedPreferences(PREFERENCE_NAME_USER, Context.MODE_PRIVATE)

            val editor = sharedPreferences.edit()
            editor.remove(PREF_KEY_PROFILE).apply()
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