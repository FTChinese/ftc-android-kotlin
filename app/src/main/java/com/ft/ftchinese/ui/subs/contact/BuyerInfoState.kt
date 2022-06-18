package com.ft.ftchinese.ui.subs.contact

import android.content.res.Resources
import android.util.Log
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.R
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.model.content.Following
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.model.content.TemplateBuilder
import com.ft.ftchinese.model.enums.PurchaseAction
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Address
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.store.FileStore
import com.ft.ftchinese.ui.util.ConnectionState
import com.ft.ftchinese.ui.util.connectivityState
import com.ft.ftchinese.ui.components.BaseState
import com.ft.ftchinese.ui.web.JsEventListener
import kotlinx.coroutines.*

private const val TAG = "BuyerInfo"

class BuyerInfoState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    resources: Resources,
    connState: State<ConnectionState>,
) : BaseState(scaffoldState, scope, resources, connState), JsEventListener {
    var exit by mutableStateOf(false)
        private set

    var alert by mutableStateOf("")
        private set

    var htmlLoaded by mutableStateOf("")
        private set

    fun closeAlert() {
        alert = ""
    }

    fun loadPage(
        account: Account,
        action: PurchaseAction,
        cache: FileStore,
    ) {

        if (!ensureConnected()) {
            return
        }

        val uri = Config.buildSubsConfirmUrl(account, action)
        if (uri == null) {
            Log.i(TAG, "Address url is empty")
            exit = true
            return
        }

        Log.i(TAG, "Fetching address page from $uri")

        progress.value = true
        scope.launch {

            try {
                val webContentAsync = async(Dispatchers.IO) {
                    Fetch()
                        .get(uri.toString())
                        .endText()
                        .body
                }
                val addressAsync = async(Dispatchers.IO) {
                    if (account.isEmailOnly) {
                        AccountRepo.loadAddress(account.id)
                    } else {
                        Address()
                    }
                }

                val webContent = webContentAsync.await()
                val address = addressAsync.await()

                if (webContent.isNullOrBlank() || address == null) {
                    showSnackBar(R.string.loading_failed)
                    return@launch
                }

                htmlLoaded = render(
                    account = account,
                    content = webContent,
                    address = address,
                    cache = cache,
                )
                progress.value = false
            } catch (e: Exception) {
                e.message?.let {
                    Log.i(TAG, it)
                    showSnackBar(it)
                }
                progress.value = false
            }
        }
    }

    private suspend fun render(
        account: Account,
        content: String,
        address: Address,
        cache: FileStore,
    ): String {
        val template = withContext(Dispatchers.IO) {
            cache.readChannelTemplate()
        }

        return withContext(Dispatchers.Default) {
            TemplateBuilder(template)
                .withUserInfo(account)
                .withAddress(address)
                .withChannel(content)
                .render()
        }
    }

    override fun onClosePage() {
        exit = true
    }

    override fun onProgress(loading: Boolean) {
        progress.value = loading
    }

    override fun onAlert(message: String) {
        alert = message
    }

    override fun onTeasers(teasers: List<Teaser>) {

    }

    override fun onClickTeaser(teaser: Teaser) {}

    override fun onClickChannel(source: ChannelSource) {}

    override fun onFollowTopic(following: Following) {}
}

@Composable
fun rememberBuyerInfoState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    resources: Resources = LocalContext.current.resources,
    connState: State<ConnectionState> = connectivityState()
) = remember(scaffoldState, resources, connState) {
    BuyerInfoState(
        scaffoldState = scaffoldState,
        scope = scope,
        resources = resources,
        connState = connState
    )
}
