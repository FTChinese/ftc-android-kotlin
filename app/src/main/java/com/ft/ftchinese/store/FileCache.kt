package com.ft.ftchinese.store

import android.content.Context
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.jakewharton.byteunits.BinaryByteUnit
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.io.File
import java.io.FileInputStream

object CacheFileNames {
    const val stripePrices = "subs_stripe_prices.${BuildConfig.VERSION_CODE}.json"
    const val stripeCustomer = "account_stripe_customer.${BuildConfig.VERSION_CODE}.json"
    const val wxAvatar = "wx_avatar.jpg"
    const val splashSchedule = "splash_schedule.json"

    fun paywallFile(isTest: Boolean): String {
        return "subs_paywall.${BuildConfig.VERSION_CODE}.${if (isTest) "test" else "live"}.json"
    }
}

val templateCache: MutableMap<String, String> = HashMap()

class FileCache (private val context: Context) : AnkoLogger {

    fun saveText(name: String, text: String) {
        try {
            File(context.filesDir, name).writeText(text)
        } catch (e: Exception) {
           info("Failed to save file $name due to ${e.message}")
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
            info("Cannot open file $name due to: ${e.message}")
            null
        }
    }

    fun writeBinaryFile(name: String, array: ByteArray) {
        try {
            File(context.filesDir, name).writeBytes(array)
        } catch (e: Exception) {
            info("Failed to save binary file $name due to ${e.message}")
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

    fun deleteFile(name: String?) {
        if (name == null) {
            return
        }

        try {
            context.deleteFile(name)
        } catch (e: Exception) {
            info(e)
        }
    }

    /**
     * Read files from the the `raw` directory of the package.
     */
    private fun readRaw(name: String, resId: Int): String {
        val cached = templateCache[name]
        if (cached != null) {
            info("Using cached template: $name")

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

