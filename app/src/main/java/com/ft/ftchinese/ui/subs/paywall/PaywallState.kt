package com.ft.ftchinese.ui.subs.paywall

import android.content.res.Resources
import android.util.Log
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.paywall.defaultPaywall
import com.ft.ftchinese.store.FileStore
import com.ft.ftchinese.ui.util.ConnectionState
import com.ft.ftchinese.ui.util.connectivityState
import com.ft.ftchinese.ui.components.BaseState
import com.ft.ftchinese.ui.repo.PaywallRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "Paywall"

class PaywallState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    resources: Resources,
    connState: State<ConnectionState>,
) : BaseState(scaffoldState, scope, resources, connState) {

    var refreshing by mutableStateOf(false)
        private set

    var paywallData by mutableStateOf(defaultPaywall)
        private set

    fun loadPaywall(
        isTest: Boolean,
        cache: FileStore
    ) {
        progress.value = true
        scope.launch {
            val pw = PaywallRepo.fromFileCache(isTest, cache)

            var cacheFound = false
            if (pw != null) {
                Log.i(TAG, "Paywall data loaded from local cached file")
                cacheFound = true
                paywallData = pw
            }

            // If paywall is not found in cache,
            // indicate we are retrieving from server;
            // otherwise silently update.
            progress.value = !cacheFound

            if (!isConnected) {
                progress.value = false
                return@launch
            }

            val result = PaywallRepo.fromServer(
                isTest = isTest,
                scope = scope,
                cache = cache,
            )
            progress.value = false

            when (result) {
                is FetchResult.LocalizedError -> {
                    if (!cacheFound) {
                        showSnackBar(result.msgId)
                    }
                }
                is FetchResult.TextError -> {
                    if (!cacheFound) {
                        showSnackBar(result.text)
                    }
                }
                is FetchResult.Success -> {
                    paywallData = result.data
                }
            }
        }
    }

    fun refreshPaywall(
        isTest: Boolean,
        cache: FileStore,
    ) {
        if (!ensureConnected()) {
            return
        }

        refreshing = true
        scope.launch {
            val result = PaywallRepo.fromServer(
                isTest = isTest,
                scope = scope,
                cache = cache
            )

            refreshing = false
            when (result) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                    showRefreshed()
                    paywallData = result.data
                }
            }
        }
    }
}

@Composable
fun rememberPaywallState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    resources: Resources = LocalContext.current.resources,
    connState: State<ConnectionState> = connectivityState()
) = remember(scaffoldState, resources, connState) {
    PaywallState(
        scaffoldState = scaffoldState,
        scope = scope,
        resources = resources,
        connState = connState
    )
}
