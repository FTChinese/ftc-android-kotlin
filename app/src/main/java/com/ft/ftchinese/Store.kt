package com.ft.ftchinese

import android.content.Context
import android.util.Log
import java.io.File

class Store {

    companion object {
        private val TAG = "Store"

        suspend fun save(context: Context?, filename: String, text: String) {

            if (context == null) {
                return
            }
            val file = File(context.filesDir, filename)

            file.writeText(text)

            Log.i(TAG, "Absolute path: ${file.absolutePath}")
            Log.i(TAG, "Canonical path: ${file.canonicalPath}")
            Log.i(TAG, "Saved file: ${file.name}")
            Log.i(TAG, "Free space: ${file.freeSpace}")
            Log.i(TAG, "Total space: ${file.totalSpace}")
            Log.i(TAG, "Usable space: ${file.usableSpace}")
        }

        suspend fun load(context: Context?, filename: String): String? {
            Log.i(TAG, "Reading file: $filename")
            if (context == null) {
                return null
            }
            try {
                return context.openFileInput(filename).bufferedReader().readText()
            } catch (e: Exception) {
                Log.i(TAG, e.toString())
            }
            return null
        }
    }
}