package com.ft.ftchinese.models

import android.content.Context
import com.ft.ftchinese.util.gson

data class Profile(
        val id: String,
        val userName: String,
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
        val updatedAt: String,
        val newsletter: Newsletter
) {
    /**
     * Save user's profile to shared preferences_profile so that we do not need to hit API every time user open's profile page.
     * Note if user edited profile data, you must re-save it to shared preferences_profile and sync it to server.
     */
    fun save(context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREFERENCE_USER_ACCOUNT, Context.MODE_PRIVATE)

        val editor = sharedPreferences.edit()
        editor.putString(PREF_KEY_PROFILE, gson.toJson(this))
                .apply()
    }

    companion object {
        private const val TAG = "profile"
        private const val PREF_KEY_PROFILE = "profile"
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