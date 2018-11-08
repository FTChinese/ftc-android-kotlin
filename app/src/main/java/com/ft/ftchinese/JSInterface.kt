package com.ft.ftchinese

import android.webkit.JavascriptInterface
import com.ft.ftchinese.models.ChannelContent
import com.ft.ftchinese.models.PagerTab
import com.ft.ftchinese.util.gson
import com.google.gson.JsonSyntaxException
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info


class JSInterface(private val pageMeta: PagerTab?) : AnkoLogger {

    private var mListener: OnEventListener? = null

    interface OnEventListener {
        fun onPageLoaded(channelDate: ChannelContent)

        fun onSelectItem(index: Int)
    }

    fun setOnEventListener(listener: OnEventListener?) {
        mListener = listener
    }

    @JavascriptInterface fun onPageLoaded(message: String) {
        try {
            val channelData = gson.fromJson<ChannelContent>(message, ChannelContent::class.java)

            mListener?.onPageLoaded(channelData)

        } catch (e: JsonSyntaxException) {
            info("Cannot parse JSON after a channel loaded")
        } catch (e: Exception) {
            info("$e")
        }
    }

    @JavascriptInterface fun onSelectItem(index: String) {
        try {
            val i = index.toInt()

            mListener?.onSelectItem(i)

        } catch (e: NumberFormatException) {
            info("$e")
        }
    }
}