package com.ft.ftchinese.ui.web

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.webkit.WebView

fun takeScreenshot(
    context: Context,
    webView: WebView,
    saveTo: Uri
): Boolean {
    val bitmap = Bitmap.createBitmap(
        webView.width,
        webView.height,
        Bitmap.Config.ARGB_8888)

    val canvas = Canvas(bitmap)
    webView.draw(canvas)

    return context
        .contentResolver
        .openOutputStream(saveTo, "w")
        ?.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, it)

            it.flush()

            bitmap.recycle()
            true
        }
        ?: false
}
