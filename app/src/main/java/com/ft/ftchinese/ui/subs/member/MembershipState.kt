package com.ft.ftchinese.ui.subs.member

import android.content.res.Resources
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.iapsubs.IAPSubsResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.stripesubs.StripeSubsResult
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.repository.AppleClient
import com.ft.ftchinese.repository.FtcPayClient
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.ui.util.ConnectionState
import com.ft.ftchinese.ui.util.connectivityState
import com.ft.ftchinese.ui.components.BaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MembershipState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    resources: Resources,
    connState: State<ConnectionState>,
) : BaseState(scaffoldState, scope, resources, connState) {
    var refreshing by mutableStateOf(false)
        private set
    
    var accountUpdated by mutableStateOf<Account?>(null)
        private set
    
    var stripeSubsUpdated by mutableStateOf<StripeSubsResult?>(null)
        private set
    
    var iapSubsUpdated by mutableStateOf<IAPSubsResult?>(null)
        private set
    
    fun refresh(account: Account) {
        // Always refresh the canonical account first so provider changes
        // made on web/backend can replace a stale local payMethod.
        refreshFromAccount(account)
    }

    private fun refreshFromAccount(account: Account) {
        if (!ensureConnected()) {
            return
        }

        refreshing = true
        scope.launch {
            val result = AccountRepo.asyncRefresh(account)
            when (result) {
                is FetchResult.LocalizedError -> {
                    refreshing = false
                    showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
                    refreshing = false
                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                    val updatedAccount = result.data
                    accountUpdated = updatedAccount

                    when {
                        updatedAccount.membership.shouldUseAddOn -> {
                            refreshing = false
                            migrateAddOn(updatedAccount)
                        }
                        updatedAccount.membership.payMethod == PayMethod.STRIPE &&
                            !updatedAccount.membership.stripeSubsId.isNullOrBlank() -> {
                            refreshStripe(updatedAccount)
                        }
                        updatedAccount.membership.payMethod == PayMethod.APPLE &&
                            !updatedAccount.membership.appleSubsId.isNullOrBlank() -> {
                            refreshIAP(updatedAccount)
                        }
                        else -> {
                            refreshing = false
                        }
                    }
                }
            }
        }
    }

    private fun migrateAddOn(a: Account) {
        if (!ensureConnected()) {
            return
        }

        scope.launch {
            val result = FtcPayClient.asyncUseAddOn(a)
            refreshing = false
            when (result) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                    showSnackBar(R.string.refresh_success)
                    accountUpdated = a.withMembership(result.data)
                }
            }
        }
    }

    private fun refreshStripe(a: Account) {
        if (!ensureConnected()) {
            return
        }

        showSnackBar(R.string.stripe_refreshing)
        refreshing = true
        scope.launch {
            val result = StripeClient.asyncRefreshSub(a)
            refreshing = false
            when (result) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                    showSnackBar(R.string.stripe_refresh_success)
                    stripeSubsUpdated = result.data
                }
            }
        }
    }

    private fun refreshIAP(a: Account) {
        if (!ensureConnected()) {
            return
        }

        showSnackBar(R.string.iap_refreshing)
        refreshing = true
        scope.launch {

            val result = AppleClient.asyncRefreshIAP(a)
            refreshing = false
            when (result) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                    showSnackBar(R.string.iap_refresh_success)
                    iapSubsUpdated = result.data
                }
            }
        }
    }

    fun cancelStripe(account: Account) {
        if (!ensureConnected()) {
            return
        }

        refreshing = true
        scope.launch {

            val result = StripeClient.asyncCancelSub(account)
            refreshing = false
            when (result) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                    showSnackBar(R.string.stripe_refresh_success)
                    stripeSubsUpdated = result.data
                }
            }
        }
    }

    fun reactivateStripe(account: Account) {
        if (!ensureConnected()) {
            return
        }

        showSnackBar(R.string.stripe_refreshing)
        refreshing = true

        scope.launch {

            val result = StripeClient.asyncReactiveSub(account)

            refreshing = false
            when (result) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                    showSnackBar(R.string.stripe_refresh_success)
                    stripeSubsUpdated = result.data
                }
            }
        }
    }
}

@Composable
fun rememberMembershipState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    resources: Resources = LocalContext.current.resources,
    connState: State<ConnectionState> = connectivityState()
) = remember(scaffoldState, resources, connState) {
    MembershipState(
        scaffoldState = scaffoldState,
        scope = scope,
        resources = resources,
        connState = connState
    )
}
