package com.ft.ftchinese.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.model.conversion.FtcCampaignItem
import com.ft.ftchinese.repository.ConversionClient
import com.ft.ftchinese.store.ConversionStore
import com.ft.ftchinese.tracking.ConversionTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConversionViewModel(app: Application): AndroidViewModel(app) {

    private val store = ConversionStore.getInstance(app)

    val campaignLiveData: MutableLiveData<FtcCampaignItem> by lazy {
        MutableLiveData<FtcCampaignItem>()
    }

    fun launchTask(retries: Int, timeout: Int, lookBackWindow: Long) {
        if (store.exists()) {
            return
        }

        viewModelScope.launch {
            val adEvent = withContext(Dispatchers.IO) {

                val tracker = ConversionTracker(
                    timeout = timeout,
                    context = getApplication()
                )

                tracker.loadAdEvent(retries)
            } ?: return@launch

            if (adEvent.isNotInLookBackWindow(lookBackWindow)) {
                Log.i(TAG, "ad  event not in lookback window")
                return@launch
            }

            val campaign = withContext(Dispatchers.IO) {
                loadCampaign(adEvent.campaignId)
            }

            Log.i(TAG, "FTC campaign loaded $campaign")

            if (campaign == null || campaign.url.isBlank()) {
                Log.i(TAG, "Campaign null")
                return@launch
            }

            campaignLiveData.value = campaign

            store.save(adEvent.campaignId)
        }
    }

    private fun loadCampaign(id: Long): FtcCampaignItem? {
        Log.i(TAG, "Loading campaign for $id")
        try {
            val schedule = ConversionClient.listCampaigns() ?: return null

            Log.i(TAG, "Campaign list $schedule")

            return schedule.sections.find {
                it.id == id.toString()
            }
        } catch (e: Exception) {
            e.message?.let { Log.i(TAG, it) }
            return null
        }
    }

    companion object {
        private const val TAG = "ConversionModelView"
    }
}
