package com.ft.ftchinese.ui.account.delete

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.R
import com.ft.ftchinese.model.request.AccountDropped
import com.ft.ftchinese.model.request.EmailPasswordParams
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.util.IntentsUtil
import com.ft.ftchinese.viewmodel.UserViewModel

@Composable
fun DeleteAccountActivityScreen(
    userViewModel: UserViewModel,
    scaffoldState: ScaffoldState,
    onDeleted: () -> Unit,
) {
    val context = LocalContext.current
    val accountState = userViewModel.accountLiveData.observeAsState()
    val account = accountState.value

    val uiState = rememberDeleteAccountState(
        scaffoldState = scaffoldState
    )

    if (account == null) {
        return
    }

    uiState.dropped?.let {
        when (it) {
            AccountDropped.Success -> {
                onDeleted()
            }
            AccountDropped.SubsExists -> {
                AlertDeleteDenied(
                    onDismiss = uiState::resetDropped,
                    onConfirm = {
                        val ok = IntentsUtil.sendDeleteAccountEmail(
                            context = context,
                            account = account
                        )
                        if (!ok) {
                            uiState.showSnackBar(R.string.prompt_no_email_app)
                        }
                        uiState.resetDropped()
                    }
                )
            }
        }
    }

    ProgressLayout(
        loading = uiState.progress.value,
        modifier = Modifier.fillMaxSize()
    ) {
        DeleteAccountScreen(
            loading = uiState.progress.value,
            onVerify = {
                uiState.drop(
                    ftcId = account.id,
                    params = EmailPasswordParams(
                        email = account.email,
                        password = it,
                    )
                )
            }
        )
    }
}

