package com.ft.ftchinese.model.content

import android.content.Context
import kotlinx.serialization.Serializable
import java.net.URLEncoder

private val followingTemplate: Map<String, String> = hashMapOf(
    "'{follow-tags}'" to "tag",
    "'{follow-topics}'" to "topic",
    "'{follow-areas}'" to "area",
    "'{follow-industries}'" to "industry",
    "'{follow-authors}'" to "author",
    "'{follow-columns}'" to "column"
)

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
@Serializable
data class Following(
        var type: String, // JS uses this value. Possible values: `tag`, `topic`, `industry`, `area`, `author`, `column`.
        var tag: String, // This is the string shown along with the FOLLOW button
        var action: String // `follow` or `unfollow`. Used to determine if user followed or unfollowed something.
) {
    val topic: String
        get() = URLEncoder.encode("${type}_$tag", "utf-8")
}

/**
 * This is used to manage saving and loading of data
 * into/from shared preference for 关注 button in WebView.
 * It is used by the inner class ContentWebViewInterface in
 * AbsContentActivity, and FollowingActivity.
 *
 * Storage mechanism: use Following#type as preference's key. It value is a HashSet.
 * The HashSet saves the value of Following#tag under the save Following#type.
 */
class FollowingManager private constructor(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(PREF_FILE_FOLLOWING, Context.MODE_PRIVATE)
    private val editor = sharedPreferences.edit()

    fun save(following: Following): Boolean {
        // Since Following#type is used as key, it must not be empty or null.
        // Even though Kotlin says call null check is redundant,
        // Gson does not work well with Kotlin's type system.
        // It insist on parsing Java's null value into Kotlin's non-null field.
        if (following.type.isBlank()) {
            return false
        }

        // Use Following#type as preferences' key
        val hs = sharedPreferences.getStringSet(following.type, HashSet<String>())

        // Create a new HashSet from the original one.
        val newHs = hs?.let { HashSet(it) }

        // Use the value of Following#tag as the the value of a HashSet
        var isSubscribed = false
        when (following.action) {
            ACTION_FOLLOW -> {
                isSubscribed = true
                newHs?.add(following.tag)
            }

            ACTION_UNFOLLOW -> {
                newHs?.remove(following.tag)
            }
        }

        // Save the updated HashSet
        editor.putStringSet(following.type, newHs)
        editor.apply()

        return isSubscribed
    }

    fun loadTemplateCtx(): Map<String, String> {
        return followingTemplate.mapValues { (_, value) ->
            try {
                val ss = sharedPreferences.getStringSet(value, null) ?: setOf()

                if (ss.isEmpty()) {
                    ""
                } else {
                    ss.joinToString { "'$it'" }
                }

            } catch (e: Exception) {
                ""
            }
        }
    }

    // Load data as a list to build recycler view.
    fun load(): List<Following> {
        val result = mutableListOf<Following>()

        followingTemplate.forEach { (_, value) ->
            try {
                val ss = sharedPreferences.getStringSet(value, setOf()) ?: setOf()

                ss.forEach {
                    result.add(Following(type = value, tag = it, action = ACTION_UNFOLLOW))
                }
            } catch (e: Exception) {

            }
        }

        return result
    }

    companion object {
        const val ACTION_FOLLOW = "follow"
        const val ACTION_UNFOLLOW = "unfollow"
        const val PREF_FILE_FOLLOWING = "following"

        private var instance: FollowingManager? = null

        @Synchronized fun getInstance(ctx: Context): FollowingManager {
            if (instance == null) {
                instance = FollowingManager(ctx.applicationContext)
            }
            return instance!!
        }
    }
}

