package com.ft.ftchinese.util

import android.content.Context
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

class FileCache (private val context: Context) : AnkoLogger {

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
}

