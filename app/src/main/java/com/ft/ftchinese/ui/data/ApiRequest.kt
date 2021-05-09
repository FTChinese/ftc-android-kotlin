package com.ft.ftchinese.ui.data

import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.ClientError
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.repository.AccountRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

object ApiRequest : AnkoLogger {
    // Refresh a user's account data, regardless of logged in
    // via email or wecaht.
    suspend fun asyncRefresh(account: Account): FetchResult<Account> {
        try {

            val updatedAccount = withContext(Dispatchers.IO) {
                AccountRepo.refresh(account)
            } ?: return FetchResult.LocalizedError(R.string.loading_failed)

            return FetchResult.Success(updatedAccount)
        } catch (e: ClientError) {

            info("Refresh account api error $e")

            return if (e.statusCode == 404) {
                FetchResult.LocalizedError(R.string.api_account_not_found)
            } else {
                FetchResult.fromServerError(e)
            }
        } catch (e: Exception) {
            info("Refresh account exception $e")
            return FetchResult.fromException(e)
        }
    }
}