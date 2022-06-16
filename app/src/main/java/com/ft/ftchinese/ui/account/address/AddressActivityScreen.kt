package com.ft.ftchinese.ui.account.address

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.viewmodel.UserViewModel

@Composable
fun AddressActivityScreen(
    userViewModel: UserViewModel,
    scaffoldState: ScaffoldState,
) {
    val accountState = userViewModel.accountLiveData.observeAsState()
    val account = accountState.value

    val uiState = rememberAddressState(
        scaffoldState = scaffoldState
    )

    if (account == null) {
        return
    }

    LaunchedEffect(key1 = Unit) {
        uiState.load(account.id)
    }

    ProgressLayout(
        loading = uiState.progress.value,
        modifier = Modifier.fillMaxSize()
    ) {
        AddressScreen(
            address = uiState.currentAddress.value,
            loading = uiState.progress.value,
            onSave = {
                uiState.update(
                    ftcId = account.id,
                    address = it,
                )
            }
        )
    }
}
