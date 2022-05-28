package com.ft.ftchinese.ui.wxlink

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.ui.base.ConnectionState
import com.ft.ftchinese.ui.base.connectivityState
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.dialog.ScopedBottomSheetDialogFragment
import com.ft.ftchinese.ui.theme.OTheme
import com.ft.ftchinese.viewmodel.UserViewModel

/**
 * Show details of account to be bound, show a button to let
 * user to confirm the performance, or just deny accounts merging.
 * It has 2 usages:
 * 1. Wx-only user tries to link to an existing email account
 * 2. Email user wants to link to wechat.
 */
class LinkPreviewFragment(
    private val params: WxEmailLinkAccounts
) : ScopedBottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                OTheme {
                    LinkFragmentScreen(
                        params = params,
                        onSuccess = {
                            activity?.apply {
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        },
                        onBack = {
                            dismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LinkFragmentScreen(
    params: WxEmailLinkAccounts,
    onSuccess: () -> Unit,
    onBack: () -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    Scaffold(
        topBar = {
            Toolbar(
                heading = "",
                onBack = onBack,
                icon = Icons.Default.Close
            )
        },
        scaffoldState = scaffoldState
    ) { innerPadding ->
        WxLinkEmailScreen(
            linkParams = params,
            innerPadding = innerPadding,
            scaffoldState = scaffoldState,
            onSuccess = onSuccess,
        )
    }
}

@Composable
fun WxLinkEmailScreen(
    userViewModel: UserViewModel = viewModel(),
    linkParams: WxEmailLinkAccounts,
    scaffoldState: ScaffoldState,
    innerPadding: PaddingValues,
    onSuccess: () -> Unit
) {

    val connection by connectivityState()
    val isConnected = connection == ConnectionState.Available
    val linkState = rememberLinkState(scaffoldState = scaffoldState)

    LaunchedEffect(key1 = linkState.accountUpdated) {
        linkState.accountUpdated?.let {
            userViewModel.saveAccount(it)
        }
    }

    if (linkState.accountUpdated != null) {
        LinkResultScreen(
            onFinish = onSuccess
        )
    } else {
        ProgressLayout(
            loading = linkState.progress.value,
            modifier = Modifier.padding(innerPadding)
        ) {
            LinkScreen(
                loading = linkState.progress.value,
                params = linkParams,
                onLink = {
                    if (!isConnected) {
                        linkState.showNotConnected()
                        return@LinkScreen
                    }
                    linkState.link(it)
                }
            )
        }
    }
}
