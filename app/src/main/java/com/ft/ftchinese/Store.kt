package com.ft.ftchinese

import android.content.Context
import android.content.res.Resources
import android.util.Log
import java.io.File

class Store {

    companion object {
        private val TAG = "Store"

        fun save(context: Context?, filename: String?, text: String?) {

            if (context == null || filename == null || text == null) {
                return
            }
            val file = File(context.filesDir, filename)

            file.writeText(text)

            Log.i(TAG, "Saved file: ${file.name}. Canonical path: ${file.canonicalPath}")
        }

        fun load(context: Context?, filename: String?): String? {
            Log.i(TAG, "Reading file: $filename")
            if (context == null || filename == null) {
                return null
            }
            try {
                return context.openFileInput(filename).bufferedReader().readText()
            } catch (e: Exception) {
                Log.i(TAG, e.toString())
            }
            return null
        }

        fun readRawFile(resources: Resources, resId: Int): String? {

            try {
                Log.i(TAG, "Reading raw file")
                val input = resources.openRawResource(resId)
                return input.bufferedReader().use { it.readText() }

            } catch (e: ExceptionInInitializerError) {
                Log.w("readHtml", e.toString())
            }
            return null
        }


    }
}

