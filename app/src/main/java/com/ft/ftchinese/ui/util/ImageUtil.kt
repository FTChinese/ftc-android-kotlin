package com.ft.ftchinese.ui.util

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.ByteArrayOutputStream

object ImageUtil {
    // Build a path to pictures directory like:
    // content://media/external_primary/images/media
    fun getFilePath(): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY
            )
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
    }

    fun saveScreenshot(
        contentResolver: ContentResolver,
        bitmap: Bitmap,
        to: Uri
    ): Boolean {
        return contentResolver.openOutputStream(to, "w")
            ?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, it)

                it.flush()

                bitmap.recycle()
                true
            }
            ?: false
    }

    // See https://ngengesenior.medium.com/pick-image-from-gallery-in-jetpack-compose-5fa0d0a8ddaf
    // https://developer.android.com/reference/android/provider/MediaStore.Images.Media
    // https://developer.android.com/reference/android/graphics/ImageDecoder#createSource(android.content.ContentResolver,%20android.net.Uri)
    fun loadAsBitmap(cr: ContentResolver, uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT < 28) {
            MediaStore.Images.Media.getBitmap(cr, uri)
        } else {
            val source = ImageDecoder.createSource(cr, uri)
            ImageDecoder.decodeBitmap(source)
        }
    }

    fun bitmapToByteArray(bmp: Bitmap, format: Bitmap.CompressFormat): ByteArray {
        return ByteArrayOutputStream().use { stream ->
            bmp.compress(format, 100, stream)
            bmp.recycle()
            stream.toByteArray()
        }
    }
}
