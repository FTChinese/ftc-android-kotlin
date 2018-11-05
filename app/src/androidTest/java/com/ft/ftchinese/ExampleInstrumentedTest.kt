package com.ft.ftchinese

import android.net.Uri
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.util.Log

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        assertEquals("com.ft.ftchinese", appContext.packageName)
    }

    @Test fun query() {
        val uri = Uri.Builder()
                .appendQueryParameter("app_id", "abc")
                .appendQueryParameter("charset", "UTF-8")
                .appendQueryParameter("method", "alipay.trade.app.pay")
                .appendQueryParameter("sign_type", "RSA2")
                .appendQueryParameter("timestamp", "2018-09-13 13:44:30")
                .appendQueryParameter("version", "1.0")
                .build()

        println(uri.query)
        println(uri.encodedQuery)

    }

    @Test fun parseUri() {
        val uri = Uri.parse("http://www.ftchinese.com/channel/tradewar.html")
        Log.i("TestURI", uri.pathSegments.toString())
        Log.i("TestURI", uri.path)
    }
}
