package com.ft.ftchinese.ui.account.address

import android.content.res.Resources
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Address
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.ui.util.ConnectionState
import com.ft.ftchinese.ui.util.connectivityState
import com.ft.ftchinese.ui.components.BaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AddressState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    resources: Resources,
    connState: State<ConnectionState>,
) : BaseState(scaffoldState, scope, resources, connState) {

    val currentAddress = mutableStateOf<Address?>(null)

    fun load(ftcId: String) {
        if (!ensureConnected()) {
            return
        }

        progress.value = true
        scope.launch {
            val address = AccountRepo.asyncLoadAddress(ftcId)
            if (address != null) {
                currentAddress.value = address
            }
            progress.value = false
        }
    }

    fun update(ftcId: String, address: Address) {
        if (!ensureConnected()) {
            return
        }

        progress.value = true
        scope.launch {
            val result = AccountRepo.asyncUpdateAddress(
                ftcId = ftcId,
                address = address,
            )

            progress.value = false
            when (result) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                    currentAddress.value = result.data
                    showSnackBar(R.string.prompt_saved)
                }
            }
        }
    }
}

@Composable
fun rememberAddressState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    resources: Resources = LocalContext.current.resources,
    connState: State<ConnectionState> = connectivityState()
) = remember(scaffoldState, resources) {
    AddressState(
        scaffoldState = scaffoldState,
        scope = scope,
        resources = resources,
        connState = connState
    )
}
