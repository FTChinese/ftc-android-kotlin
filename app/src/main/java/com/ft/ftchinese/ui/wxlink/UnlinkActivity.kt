package com.ft.ftchinese.ui.wxlink

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.base.ConnectionState
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.connectivityState
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.theme.OTheme
import com.ft.ftchinese.viewmodel.UserViewModel

class UnlinkActivity : ScopedAppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OTheme {
                val scaffoldState = rememberScaffoldState()

                Scaffold(
                    topBar = {
                        Toolbar(
                            heading = stringResource(id = R.string.title_unlink),
                            onBack = {
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        )
                    }
                ) { innerPadding ->
                    UnlinkActivityScreen(
                        scaffoldState = scaffoldState,
                        innerPadding = innerPadding
                    ) {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newIntent(context: Context): Intent {
            return Intent(context, UnlinkActivity::class.java)
        }
    }
}

@Composable
private fun UnlinkActivityScreen(
    userViewModel: UserViewModel = viewModel(),
    scaffoldState: ScaffoldState,
    innerPadding: PaddingValues,
    onSuccess: () -> Unit
) {
    val connection by connectivityState()
    val isConnected = connection == ConnectionState.Available

    val accountState = userViewModel.accountLiveData.observeAsState()
    val account = accountState.value

    val linkState = rememberLinkState(scaffoldState = scaffoldState)

    if (account == null) {
        return
    }

    LaunchedEffect(key1 = linkState.accountUpdated) {
        linkState.accountUpdated?.let {
            userViewModel.saveAccount(it)
            onSuccess()
        }
    }

    ProgressLayout(
        loading = linkState.progress.value,
        modifier = Modifier.padding(innerPadding)
    ) {
        UnlinkScreen(
            account = account,
            loading = linkState.progress.value,
            onUnlink = {
                if (!isConnected) {
                    linkState.showNotConnected()
                    return@UnlinkScreen
                }

                linkState.unlink(
                    account = account,
                    anchor = it,
                )
            }
        )
    }
}

