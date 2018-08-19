package com.ft.ftchinese.models

import android.content.Context
import android.util.Log

/**
 * Used to parse messages passed from JS when user clicked `FOLLOW` button.
 * Those data are stored locally.
 * Example saved XML:
 * ```xml
 * <map>
 *  <set name="tag">
 *      <string>美国</string>
 *      <string>中国经济</string>
 *      <string>特朗普</string>
 *   </set>
 * </map>
 * ```
 */
data class Following(
        var type: String, // JS uses this value. Possible values: `tag`, `topic`, `industry`, `area`, `augthor`, `column`. `augthor` is a typo in JS code, but you have to keep that typo on.
        var tag: String, // This is the string show along with the FOLLOW button
        var action: String // `follow` or `unfollow`. Used to determine if user if follow or unfollow something.
) {
    fun save(context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME_FOLLOWING, Context.MODE_PRIVATE)

        val hs = sharedPreferences.getStringSet(type, HashSet<String>())

        Log.i(TAG, "Current shared prefernce: $hs")

        val newHs = HashSet(hs)

        when (action) {
            FOLLOWING_ACTION_ADD -> {
                newHs.add(tag)
            }

            FOLLOWING_ACTION_REMOVE -> {
                newHs.remove(tag)
            }
        }

        Log.i(TAG, "New set: $newHs")

        val editor = sharedPreferences.edit()
        editor.putStringSet(type, newHs)
        editor.apply()
    }

    companion object {
        private const val TAG = "Following"
        const val FOLLOWING_ACTION_ADD = "follow"
        const val FOLLOWING_ACTION_REMOVE = "unfollow"

        // Keys used in shared preferences
        val keys = arrayOf("tag", "topic", "area", "industry", "author", "column")
        const val PREF_NAME_FOLLOWING = "following"

        /**
         * Load whatever a user put in favorite collection.
         * Returns a map of strings.
         * The key is from `keys`.
         * The value is a string join from a Set.
         * The value will be passed to a JS object in HTML file.
         */
        fun loadAsMap(context: Context): Map<String, String> {
            val sharedPreferences = context.getSharedPreferences(PREF_NAME_FOLLOWING, Context.MODE_PRIVATE)

            return keys.associate {
                val ss = sharedPreferences.getStringSet(it, setOf())

                val v = ss.joinToString {
                    "'$it'"
                }

                Pair(it, v)
            }
        }

        fun loadAsList(context: Context?): MutableList<Following>? {
            val sharedPreferences = context?.getSharedPreferences(PREF_NAME_FOLLOWING, Context.MODE_PRIVATE) ?: return null

            val result = mutableListOf<Following>()
            for (key in keys) {
                val ss = sharedPreferences.getStringSet(key, setOf())
                for (elem in ss) {
                    result.add(Following(type = key, tag = elem, action = FOLLOWING_ACTION_REMOVE))
                }
            }

            return result
        }
    }
}