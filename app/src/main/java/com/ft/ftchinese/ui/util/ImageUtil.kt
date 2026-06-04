package com.ft.ftchinese.ui.util

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
                val ok = bitmap.compress(Bitmap.CompressFormat.JPEG, 50, it)

                it.flush()

                bitmap.recycle()
                ok
            }
            ?: false
    }

    // Decode from the content stream to avoid ImageDecoder failures on some MediaStore URIs.
    fun loadAsBitmap(cr: ContentResolver, uri: Uri): Bitmap? {
        return cr.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
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
