package com.ft.ftchinese.service

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.ft.ftchinese.model.reader.ReadingDuration
import com.ft.ftchinese.repository.AuthClient

const val KEY_DUR_URL = "url"
const val KEY_DUR_REFER = "refer"
const val KEY_DUR_START = "star_unix"
const val KEY_DUR_END = "end_unix"
const val KEY_DUR_USER_ID = "user_id"

private const val TAG = "ReadingDurationWorker"

class ReadingDurationWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        Log.i(TAG, "Start running ReadingDurationWorker")
        val url = inputData.getString(KEY_DUR_URL) ?: return Result.success()
        val refer = inputData.getString(KEY_DUR_REFER) ?: return Result.success()
        val startUnix = inputData.getLong(KEY_DUR_START, 0)
        val endUnix = inputData.getLong(KEY_DUR_END, 0)
        val userId = inputData.getString(KEY_DUR_USER_ID) ?: return Result.success()

        try {
            AuthClient.engaged(ReadingDuration(
                url = url,
                refer = refer,
                startUnix = startUnix,
                endUnix = endUnix,
                userId = userId,
                functionName = "onLoad"
            ))
        } catch (e: Exception) {
            Log.i(TAG, "Error when tracking reading duration $e")
            return Result.retry()
        }

        return Result.success()
    }
}
