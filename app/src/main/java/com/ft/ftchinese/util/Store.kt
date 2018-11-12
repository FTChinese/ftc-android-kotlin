package com.ft.ftchinese.util

import android.content.Context
import android.content.res.Resources
import android.util.Log
import com.ft.ftchinese.R
import com.jakewharton.byteunits.BinaryByteUnit
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

object Store {

    private const val TAG = "Store"

    fun save(context: Context?, filename: String?, text: String?): Job? {

        if (context == null || filename.isNullOrBlank() || text.isNullOrBlank()) {
            return null
        }
        val file = File(context.filesDir, filename)

        return GlobalScope.launch {
            try {
                file.writeText(text)

                Log.i(TAG, "Saved file: ${file.name}. Canonical path: ${file.canonicalPath}")
            } catch (e: IOException) {
                Log.i(TAG, "Failed to save ${file.name}. Reason: $e")
            }
        }
    }

    fun exists(context: Context, filename: String): Boolean {
        return try {
            File(context.filesDir, filename).exists()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun load(context: Context?, filename: String?): String? {
        Log.i(TAG, "Reading file: $filename")
        if (context == null || filename.isNullOrBlank()) {
            return null
        }
        return try {
            GlobalScope.async {
                context.openFileInput(filename).bufferedReader().readText()
            }.await()

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
    private fun readRawFile(resources: Resources, resId: Int): String? {

        return try {
            val input = resources.openRawResource(resId)
            input.bufferedReader().use { it.readText() }
        } catch (e: ExceptionInInitializerError) {
            null
        }
    }

    suspend fun readChannelTemplate(resources: Resources): String? {
        return GlobalScope.async {
            readRawFile(resources, R.raw.list)
        }.await()
    }

    suspend fun readStoryTemplate(resources: Resources): String? {
        return GlobalScope.async {
            readRawFile(resources, R.raw.story)
        }.await()
    }
}

