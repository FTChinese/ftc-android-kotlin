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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.R
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
    private val params: WxEmailLink
) : ScopedBottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                OTheme {
                    val scaffoldState = rememberScaffoldState()
                    Scaffold(
                        topBar = {
                            Toolbar(
                                heading = "",
                                onBack = { dismiss() },
                                icon = Icons.Default.Close
                            )
                        },
                        scaffoldState = scaffoldState
                    ) { innerPadding ->
                        LinkPreviewScreen(
                            linkParams = params,
                            innerPadding = innerPadding,
                            scaffoldState = scaffoldState,
                            onSuccess = {
                                activity?.apply {
                                    setResult(Activity.RESULT_OK)
                                    finish()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

}

@Composable
fun LinkPreviewScreen(
    userViewModel: UserViewModel = viewModel(),
    linkParams: WxEmailLink,
    scaffoldState: ScaffoldState,
    innerPadding: PaddingValues,
    onSuccess: () -> Unit
) {

    val connection by connectivityState()
    val isConnected = connection == ConnectionState.Available
    val linkState = rememberLinkState(scaffoldState = scaffoldState)

    linkState.linked.value?.let {
        userViewModel.saveAccount(it)
    }

    if (linkState.linked.value != null) {
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
                        linkState.showSnackBar(R.string.prompt_no_network)
                        return@LinkScreen
                    }
                    linkState.link(it)
                }
            )
        }
    }
}
