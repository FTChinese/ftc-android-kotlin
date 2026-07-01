package com.ft.ftchinese.ui.subs.catalog

import android.content.Context
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.subscriptioncatalog.SubscriptionCatalog
import com.ft.ftchinese.repository.ApiConfig
import com.ft.ftchinese.tracking.PaywallTracker
import com.ft.ftchinese.ui.components.BaseState
import com.ft.ftchinese.ui.repo.SubscriptionCatalogRepo
import com.ft.ftchinese.ui.subs.SubscriptionEntryIntent
import com.ft.ftchinese.ui.util.ConnectionState
import com.ft.ftchinese.ui.util.connectivityState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SubscriptionCatalogState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    connState: State<ConnectionState>,
    context: Context,
) : BaseState(scaffoldState, scope, context.resources, connState) {

    var refreshing by mutableStateOf(false)
        private set

    var catalog by mutableStateOf<SubscriptionCatalog?>(null)
        private set

    var loaded by mutableStateOf(false)
        private set

    fun loadCatalog(
        api: ApiConfig,
        userId: String?,
        entry: SubscriptionEntryIntent? = null,
    ) {
        progress.value = true
        scope.launch {
            if (!isConnected) {
                progress.value = false
                return@launch
            }

            when (val result = SubscriptionCatalogRepo.fromServer(
                api = api,
                userId = userId,
                ccode = if (entry == null) PaywallTracker.campaignCcode() else entry.campaignCcode(),
                tier = entry?.tier,
                offerHint = entry?.offerHint,
                discountFrom = entry?.from,
            )) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                    catalog = result.data
                    loaded = true
                }
            }
            progress.value = false
        }
    }

    fun refreshCatalog(
        api: ApiConfig,
        userId: String?,
        entry: SubscriptionEntryIntent? = null,
    ) {
        if (!ensureConnected()) {
            return
        }

        refreshing = true
        scope.launch {
            when (val result = SubscriptionCatalogRepo.fromServer(
                api = api,
                userId = userId,
                ccode = if (entry == null) PaywallTracker.campaignCcode() else entry.campaignCcode(),
                tier = entry?.tier,
                offerHint = entry?.offerHint,
                discountFrom = entry?.from,
            )) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                    catalog = result.data
                    loaded = true
                    showRefreshed()
                }
            }
            refreshing = false
        }
    }
}

@Composable
fun rememberSubscriptionCatalogState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    connState: State<ConnectionState> = connectivityState(),
    context: Context = LocalContext.current,
) = remember(scaffoldState, connState) {
    SubscriptionCatalogState(
        scaffoldState = scaffoldState,
        scope = scope,
        connState = connState,
        context = context
    )
}
