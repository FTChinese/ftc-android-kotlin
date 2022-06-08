package com.ft.ftchinese.store

import android.content.Context
import com.ft.ftchinese.model.content.Following

private val followingTemplate: Map<String, String> = hashMapOf(
    "'{follow-tags}'" to "tag",
    "'{follow-topics}'" to "topic",
    "'{follow-areas}'" to "area",
    "'{follow-industries}'" to "industry",
    "'{follow-authors}'" to "author",
    "'{follow-columns}'" to "column"
)

/**
 * This is used to manage saving and loading of data
 * into/from shared preference for 关注 button in WebView.
 * It is used by the inner class ContentWebViewInterface in
 * AbsContentActivity, and FollowingActivity.
 *
 * Storage mechanism: use Following#type as preference's key. It value is a HashSet.
 * The HashSet saves the value of Following#tag under the save Following#type.
 */
class FollowedTopics private constructor(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(PREF_FILE_FOLLOWING,
        Context.MODE_PRIVATE
    )
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

        private var instance: FollowedTopics? = null

        @Synchronized fun getInstance(ctx: Context): FollowedTopics {
            if (instance == null) {
                instance = FollowedTopics(ctx.applicationContext)
            }
            return instance!!
        }
    }
}
