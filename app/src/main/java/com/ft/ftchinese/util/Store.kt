package com.ft.ftchinese.util

import android.content.Context
import android.content.res.Resources
import android.util.Log
import com.jakewharton.byteunits.BinaryByteUnit
import java.io.File

object Store {

    private val TAG = "Store"

    fun save(context: Context?, filename: String?, text: String?) {

        if (context == null || filename == null || text == null) {
            return
        }
        val file = File(context.filesDir, filename)

        file.writeText(text)

        Log.i(TAG, "Saved file: ${file.name}. Canonical path: ${file.canonicalPath}")
    }

    fun exists(context: Context, filename: String): Boolean {
        return try {
            File(context.filesDir, filename).exists()
        } catch (e: Exception) {
            false
        }
    }

    fun load(context: Context?, filename: String?): String? {
        Log.i(TAG, "Reading file: $filename")
        if (context == null || filename == null) {
            return null
        }
        return try {
            context.openFileInput(filename).bufferedReader().readText()
        } catch (e: Exception) {
            Log.i(TAG, e.toString())

            null
        }
    }

    fun filesSpace(context: Context?): String? {
        if (context == null) return null

        return try {
            val filesSize = context.filesDir.listFiles()
                    .map {

                        if (!it.isFile) 0 else it.length()
                    }
                    .fold(0L) { total, next -> total + next}


            BinaryByteUnit.format(filesSize)
        } catch (e: Exception) {
            Log.i(TAG, e.toString())
            null
        }
    }

    fun clearFiles(context: Context?): Boolean {
        if (context == null) return false

        return try {
            for (name in context.fileList()) {
                context.deleteFile(name)
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Read files from the the `raw` directory of the package.
     */
    fun readRawFile(resources: Resources, resId: Int): String? {

        return try {
            Log.i(TAG, "Reading raw file")
            val input = resources.openRawResource(resId)
            input.bufferedReader().use { it.readText() }

        } catch (e: ExceptionInInitializerError) {
            Log.w("readHtml", e.toString())

            null
        }
    }
}

