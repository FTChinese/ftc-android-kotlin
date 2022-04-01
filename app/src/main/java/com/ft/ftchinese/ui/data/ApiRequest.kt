package com.ft.ftchinese.ui.data

import android.util.Log
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.repository.AccountRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ApiRequest {
    private const val TAG = "ApiRequest"
    // Refresh a user's account data, regardless of logged in
    // via email or wecaht.
    suspend fun asyncRefreshAccount(account: Account): FetchResult<Account> {
        try {

            val updatedAccount = withContext(Dispatchers.IO) {
                AccountRepo.refresh(account)
            } ?: return FetchResult.LocalizedError(R.string.loading_failed)

            return FetchResult.Success(updatedAccount)
        } catch (e: APIError) {

            Log.i(TAG, "Refresh account api error $e")

            return if (e.statusCode == 404) {
                FetchResult.LocalizedError(R.string.account_not_found)
            } else {
                FetchResult.fromApi(e)
            }
        } catch (e: Exception) {
            Log.i(TAG, "Refresh account exception $e")
            return FetchResult.fromException(e)
        }
    }
}
