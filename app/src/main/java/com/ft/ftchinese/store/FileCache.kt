package com.ft.ftchinese.store

import android.content.Context
import android.util.Log
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.marshaller
import com.ft.ftchinese.model.paywall.Paywall
import com.jakewharton.byteunits.BinaryByteUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import java.io.File
import java.io.FileInputStream

object CacheFileNames {
    const val wxAvatar = "wx_avatar.jpg"
    const val splashSchedule = "splash_schedule.json"
}

// The paywall cached file are versioned therefore whenever
// app updated, it won't be bothered by json parsing failure
// caused by data structure changes.
fun paywallFileName(isTest: Boolean): String {
    return "paywall.${BuildConfig.VERSION_CODE}.${if (isTest) "test" else "live"}.json"
}

val templateCache: MutableMap<String, String> = HashMap()

private const val TAG = "FileCache"

class FileCache (private val context: Context) {

    fun saveText(name: String, text: String) {
        try {
            File(context.filesDir, name).writeText(text)
        } catch (e: Exception) {
           Log.i(TAG, "Failed to save file $name due to ${e.message}")
        }
    }

    suspend fun asyncSaveText(name: String, text: String) {
        withContext(Dispatchers.IO) {
            saveText(name, text)
        }
    }

    fun loadText(name: String?): String? {
        if (name.isNullOrBlank()) {
            return null
        }

        return try {
            context.openFileInput(name)
                .bufferedReader()
                .readText()
        } catch (e: Exception) {
            Log.i(TAG, "Cannot open file $name due to: ${e.message}")
            null
        }
    }

    suspend fun asyncLoadText(name: String): String? {
        return withContext(Dispatchers.IO) {
            loadText(name)
        }
    }

    fun savePaywall(isTest: Boolean, text: String) {
        Log.i(TAG, "Caching paywall data to file")
        saveText(paywallFileName(isTest), text = text)
    }

    suspend fun asyncLoadPaywall(isTest: Boolean): Paywall? {
        return withContext(Dispatchers.IO) {
            val data = loadText(paywallFileName(isTest = isTest))
            if (data.isNullOrBlank()) {
                null
            } else {
                try {
                    marshaller.decodeFromString<Paywall>(data)
                } catch (e: Exception) {
                    e.message?.let { Log.i(TAG, it) }
                    null
                }
            }
        }
    }

    fun writeBinaryFile(name: String, array: ByteArray) {
        try {
            File(context.filesDir, name).writeBytes(array)
        } catch (e: Exception) {
            Log.i(TAG, "Failed to save binary file $name due to ${e.message}")
        }
    }

    fun readBinaryFile(name: String): FileInputStream? {
        return try {
            context.openFileInput(name)
        } catch (e: Exception) {
            null
        }
    }

    fun exists(fileName: String): Boolean {
        return try {
            File(context.filesDir, fileName).exists()
        } catch (e: Exception) {
            false
        }
    }

    fun space(): String {
        return try {
            val fileSize = context.filesDir
                    .listFiles()
                    ?.map {
                        if (!it.isFile) 0 else it.length()
                    }
                    ?.fold(0L) { total, next -> total + next } ?: 0
            BinaryByteUnit.format(fileSize)
        } catch (e: Exception) {
            BinaryByteUnit.format(0L)
        }
    }

    suspend fun asyncSpace(): String {
        return withContext(Dispatchers.IO) {
            space()
        }
    }

    fun clear(): Boolean {
        return try {
            for (name in context.fileList()) {
                context.deleteFile(name)
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun asyncClear(): Boolean {
        return withContext(Dispatchers.IO) {
            clear()
        }
    }

    fun deleteFile(name: String?) {
        if (name == null) {
            return
        }

        try {
            context.deleteFile(name)
        } catch (e: Exception) {
            Log.i(TAG, "$e")
        }
    }

    /**
     * Read files from the the `raw` directory of the package.
     */
    private fun readRaw(name: String, resId: Int): String {
        val cached = templateCache[name]
        if (cached != null) {
            Log.i(TAG, "Using cached template: $name")

            return cached
        }

        val content = try {
            context.resources
                .openRawResource(resId)
                .bufferedReader()
                .use {
                    it.readText()
                }
        } catch (e: Exception) {
            null
        }

        return if (content != null) {
            templateCache[name] = content

            content
        } else {
            ""
        }
    }

    fun readChannelTemplate(): String {
        return readRaw("list.html", R.raw.list)
    }

    fun readStoryTemplate(): String {
        return readRaw("story.html", R.raw.story)
    }

    fun readSearchTemplate(): String {
        return readRaw("search.html", R.raw.search)
    }

    fun readGymTemplate(): String {
        return readRaw("gym.html", R.raw.gym)
    }

    fun readTestHtml(): String {
        return readRaw("test.html", R.raw.test)
    }
}

