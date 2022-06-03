package com.ft.ftchinese.repository

import android.util.Log
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.fetch.HttpResp
import com.ft.ftchinese.model.paywall.Paywall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PaywallClient {
    private const val TAG = "Paywall"

    fun retrieve(isTest: Boolean): HttpResp<Paywall> {
        val api = ApiConfig.ofSubs(isTest)
        return Fetch()
            .setBearer(api.accessToken)
            .get(api.paywall)
            .endJson(withRaw = true)
    }

    suspend fun asyncRetrieve(isTest: Boolean): FetchResult<Pair<Paywall, String>> {
        try {
            val pwResp = withContext(Dispatchers.IO) {
                retrieve(isTest)
            }

            Log.i(TAG, "Loading paywall from server finished")
            if (pwResp.body == null) {
                return FetchResult.loadingFailed
            }

            return FetchResult.Success(Pair(pwResp.body, pwResp.raw))
        } catch (e: Exception) {
            e.message?.let { Log.i(TAG, it) }
            return FetchResult.fromException(e)
        }
    }
}
