package com.ft.ftchinese.service

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ft.ftchinese.repository.SavedContentSync
import com.ft.ftchinese.repository.SavedContentSyncResult

private const val TAG = "SavedContentSyncWorker"
private const val WORK_NAME = "savedContentSync"

class SavedContentSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return when (val result = SavedContentSync.sync(applicationContext)) {
            is SavedContentSyncResult.Synced -> {
                Log.i(
                    TAG,
                    "Synced saved content: savedRemote=${result.savedRemote} " +
                            "unsavedRemote=${result.unsavedRemote} " +
                            "insertedLocal=${result.insertedLocal} " +
                            "deletedLocal=${result.deletedLocal} " +
                            "finalCount=${result.finalCount}"
                )
                Result.success()
            }
            is SavedContentSyncResult.Skipped -> {
                Log.i(TAG, "Skipped saved content sync: ${result.reason}")
                Result.success()
            }
            is SavedContentSyncResult.Failed -> {
                Log.w(TAG, "Saved content sync failed: ${result.error}")
                Result.retry()
            }
        }
    }

    companion object {
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SavedContentSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager
                .getInstance(context.applicationContext)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
