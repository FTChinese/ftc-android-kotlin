package com.ft.ftchinese

import android.content.Context
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.view.View
import com.ft.ftchinese.models.LaunchAd
import com.ft.ftchinese.models.LaunchSchedule
import com.ft.ftchinese.util.gson
import com.koushikdutta.ion.Ion
import kotlinx.android.synthetic.main.activity_ad.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.joda.time.LocalDate
import org.joda.time.format.ISODateTimeFormat

class AdActivity : AppCompatActivity(), AnkoLogger {

    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_ad)
        supportActionBar?.hide()

        // https://developer.android.com/training/system-ui/immersive
        window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION


        ad_timer.setOnClickListener {
            job?.cancel()
            MainActivity.start(this)
            finish()
        }

        val localDate = LocalDate.now()
        val today = ISODateTimeFormat.basicDate().print(localDate)

        val sharedPreferences = getSharedPreferences(LaunchSchedule.PREF_AD_SCHEDULE, Context.MODE_PRIVATE)

        val todaySchedules = sharedPreferences.getStringSet(today, null)
                ?.map {
                    gson.fromJson(it, LaunchAd::class.java)
                }

        if (todaySchedules != null && todaySchedules.isNotEmpty()) {
            val ad = todaySchedules[0]

            Ion.with(ad_image)
                    .load(ad.imageUrl)

            ad_image.setOnClickListener {
                val customTabsInt = CustomTabsIntent.Builder().build()
                customTabsInt.launchUrl(this, Uri.parse(ad.linkUrl))
                job?.cancel()
                info("Clicked ads")
            }
        }

    }

    override fun onResume() {
        super.onResume()

        setup()
    }


    private fun setup() {
        job = launch(UI) {
            for (i in 5 downTo 1) {
                ad_timer.text = "跳过 ${i}"
                delay(1000)
            }

//            MainActivity.start(this@AdActivity)
        }
    }
}
