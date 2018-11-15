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
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.io.File
import java.io.IOException

class FileCache private constructor(private val context: Context) : AnkoLogger {

    fun save(fileName: String, content: String): Job {
        return GlobalScope.launch {
            try {
                val file = File(context.filesDir, fileName)

                file.writeText(content)
            } catch (e: IOException) {
                info("Cannot cache $fileName")
            }
        }
    }

    suspend fun load(fileName: String): String {
        val job = GlobalScope.async {
            try {
                context.openFileInput(fileName)
                        .bufferedReader()
                        .readText()
            } catch (e: IOException) {
                ""
            }

        }

        return job.await()
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
                    .map {
                        if (!it.isFile) 0 else it.length()
                    }
                    .fold(0L) { total, next -> total + next }
            BinaryByteUnit.format(fileSize)
        } catch (e: Exception) {
            BinaryByteUnit.format(0L)
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

    /**
     * Read files from the the `raw` directory of the package.
     */
    private fun readRawFile(resId: Int): String {
        return try {
            context.resources
                    .openRawResource(resId)
                    .bufferedReader()
                    .use {
                        it.readText()
                    }
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun readChannelTemplate(): String {
        return GlobalScope.async {
            readRawFile(R.raw.list)
        }.await()
    }

    suspend fun readStoryTemplate(): String? {
        return GlobalScope.async {
            readRawFile(R.raw.story)
        }.await()
    }

    companion object {
        private var instance: FileCache? = null

        fun getInstance(context: Context): FileCache {
            if (instance == null) {
                instance = FileCache(context.applicationContext)
            }

            return instance!!
        }
    }
}
//object Store {
//
//    private const val TAG = "Store"
//
//    // Cache files to the data directory.
//    fun save(context: Context?, filename: String?, text: String?): Job? {
//
//        if (context == null || filename.isNullOrBlank() || text.isNullOrBlank()) {
//            return null
//        }
//        val file = File(context.filesDir, filename)
//
//        return GlobalScope.launch {
//            try {
//                file.writeText(text)
//
//                Log.i(TAG, "Saved file: ${file.name}. Canonical path: ${file.canonicalPath}")
//            } catch (e: IOException) {
//                Log.i(TAG, "Failed to save ${file.name}. Reason: $e")
//            }
//        }
//    }
//
//
//    suspend fun load(context: Context?, filename: String?): String? {
//        Log.i(TAG, "Reading file: $filename")
//        if (context == null || filename.isNullOrBlank()) {
//            return null
//        }
//        return try {
//            GlobalScope.async {
//                context.openFileInput(filename).bufferedReader().readText()
//            }.await()
//
//        } catch (e: Exception) {
//            Log.i(TAG, e.toString())
//
//            null
//        }
//    }
//}

