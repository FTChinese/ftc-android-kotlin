package com.ft.ftchinese.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ft.ftchinese.R
import com.ft.ftchinese.store.ForcedLogoutStore
import com.ft.ftchinese.ui.components.SimpleDialog
import com.ft.ftchinese.viewmodel.UserViewModel

@Composable
fun ForcedLogoutHandler(
    userViewModel: UserViewModel,
    onAfterConfirm: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val event = ForcedLogoutStore.eventLiveData.observeAsState().value

    LaunchedEffect(event?.triggeredAtMillis) {
        if (event != null) {
            userViewModel.logout()
        }
    }

    event?.let { pending ->
        SimpleDialog(
            title = stringResource(id = R.string.login_expired),
            body = stringResource(id = pending.reason.messageId),
            onDismiss = {},
            onConfirm = {
                ForcedLogoutStore.clear()
                context.startActivity(AuthActivity.newIntent(context))
                onAfterConfirm?.invoke()
            },
            confirmText = stringResource(id = R.string.btn_login),
            dismissText = null,
        )
    }
}
