package com.ft.ftchinese.repository

import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.request.Credentials
import com.ft.ftchinese.model.request.WxLinkParams
import com.ft.ftchinese.model.request.WxUnlinkParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LinkRepo {
    /**
     * Link two existing accounts.
     */
    fun link(unionId: String, params: WxLinkParams): Boolean {
        return Fetch()
            .post(Endpoint.wxLink)
            .setUnionId(unionId)
            .noCache()
            .setApiKey()
            .sendJson(params)
            .endText()
            .code == 204
    }

    suspend fun asyncLink(unionId: String, params: WxLinkParams): FetchResult<Boolean> {
        try {
            val done = withContext(Dispatchers.IO) {
                link(
                    unionId = unionId,
                    params = params
                )
            }

            return if (done) {
                FetchResult.Success(true)
            } else {
                FetchResult.loadingFailed
            }
        } catch (e: APIError) {
            return when (e.statusCode) {
                404 -> FetchResult.LocalizedError(R.string.account_not_found)
                422 -> if (e.error == null) {
                    FetchResult.fromApi(e)
                } else {
                    when {
                        e.error.isFieldAlreadyExists("account_link") -> FetchResult.LocalizedError(R.string.api_account_already_linked)
                        e.error.isFieldAlreadyExists("membership_link") -> FetchResult.LocalizedError(R.string.api_membership_already_linked)
                        e.error.isFieldAlreadyExists("membership_both_valid") -> FetchResult.LocalizedError(
                            R.string.api_membership_all_valid)
                        else -> FetchResult.fromApi(e)
                    }
                }
                else -> FetchResult.fromApi(e)
            }
        } catch (e: Exception) {
            return FetchResult.fromException(e)
        }
    }

    /**
     * Wechat user creates a new email account.
     */
    fun signUp(c: Credentials, unionId: String): Account? {
        return Fetch()
            .post(Endpoint.wxSignUp)
            .setUnionId(unionId)
            .setClient()
            .noCache()
            .setApiKey()
            .sendJson(c)
            .endJson<Account>()
            .body
    }

    fun unlink(unionId: String, params: WxUnlinkParams): Boolean {
        return Fetch()
            .post(Endpoint.wxUnlink)
            .noCache()
            .setApiKey()
            .setUnionId(unionId)
            .sendJson(params)
            .endText()
            .code == 204
    }

    suspend fun asyncUnlink(unionId: String, params: WxUnlinkParams): FetchResult<Boolean> {
        try {
            val done = withContext(Dispatchers.IO) {
                unlink(unionId, params)
            }

            return if (done) {
                FetchResult.Success(true)
            } else {
                FetchResult.LocalizedError(R.string.loading_failed)
            }
        } catch (e: APIError) {
            return when (e.statusCode) {
                404 -> FetchResult.LocalizedError(R.string.account_not_found)
                422 -> if (e.error == null) {
                    FetchResult.fromApi(e)
                } else {
                    when {
                        e.error.isFieldMissing("anchor") -> FetchResult.LocalizedError(R.string.api_anchor_missing)
                        else -> FetchResult.fromApi(e)
                    }
                }
                else -> FetchResult.fromApi(e)
            }
        } catch (e: Exception) {
            return FetchResult.fromException(e)
        }
    }
}
