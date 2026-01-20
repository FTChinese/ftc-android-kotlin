package com.ft.ftchinese.store

import android.content.Context
import android.content.SharedPreferences

object PrefsMigration {
    fun migrateIfNeeded(
        context: Context,
        oldFileName: String,
        newFileName: String,
        sentinelKey: String,
        initSentinel: ((SharedPreferences.Editor) -> Unit)? = null
    ): SharedPreferences? {
        val newPrefs = try {
            SecurePrefs.encrypted(context, newFileName)
        } catch (e: Exception) {
            return null
        }

        if (newPrefs.contains(sentinelKey)) {
            return newPrefs
        }

        val oldPrefs = context.getSharedPreferences(oldFileName, Context.MODE_PRIVATE)
        if (oldPrefs.all.isNotEmpty()) {
            val editor = newPrefs.edit()
            copyAll(oldPrefs, editor)
            val success = editor.commit()
            if (success) {
                oldPrefs.edit().clear().commit()
            }
        } else if (initSentinel != null) {
            val editor = newPrefs.edit()
            initSentinel(editor)
            editor.commit()
        }

        return newPrefs
    }

    private fun copyAll(oldPrefs: SharedPreferences, editor: SharedPreferences.Editor) {
        for ((key, value) in oldPrefs.all) {
            when (value) {
                is String -> editor.putString(key, value)
                is Int -> editor.putInt(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Float -> editor.putFloat(key, value)
                is Long -> editor.putLong(key, value)
                is Set<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    editor.putStringSet(key, value as Set<String>)
                }
            }
        }
    }
}
