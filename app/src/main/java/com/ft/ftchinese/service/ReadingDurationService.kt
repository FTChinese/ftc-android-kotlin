package com.ft.ftchinese.service

import android.app.IntentService
import android.content.Context
import android.content.Intent
import com.ft.ftchinese.model.reader.ReadingDuration
import com.ft.ftchinese.repository.ReaderRepo

const val EXTRA_READING_DURATION = "extra_reading_duration"

/**
 * A constructor is required, and must call the super [android.app.IntentService]
 * constructor with a name for the worker thread.
 */
class ReadingDurationService : IntentService("ReadingDurationIntentService") {

    /**
     * The IntentService calls this method from the default worker thread with
     * the intent that started the service. When this method returns, IntentService
     * stops the service, as appropriate.
     */
    override fun onHandleIntent(intent: Intent?) {
        val dur = intent?.getParcelableExtra<ReadingDuration>(EXTRA_READING_DURATION) ?: return

        ReaderRepo.getInstance().engaged(dur)
    }

    companion object {
        fun start(context: Context?, dur: ReadingDuration) {
            context?.startService(
                    Intent(
                            context,
                            ReadingDurationService::class.java
                    ).apply {
                        putExtra(EXTRA_READING_DURATION, dur)
                    }
            )
        }
    }
}
