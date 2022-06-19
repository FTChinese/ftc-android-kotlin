package com.ft.ftchinese.ui.subs

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.subs.member.MemberActivityScreen
import com.ft.ftchinese.ui.theme.OTheme
import com.ft.ftchinese.ui.util.IntentsUtil
import com.ft.ftchinese.viewmodel.UserViewModel
import kotlinx.coroutines.launch

class MemberActivity : ComponentActivity() {

    private var refreshed: Boolean = false
    private lateinit var userViewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]

        setContent {
            OTheme {
                val scaffoldState = rememberScaffoldState()
                val scope = rememberCoroutineScope()

                Scaffold(
                    topBar = {
                        Toolbar(
                            heading = stringResource(id = R.string.title_my_subs),
                            onBack = {
                                // In case the activity is called from article's barrier.
                                if (refreshed) {
                                    // TODO: this could only be triggered by back button, not by back press.
                                    setResult(Activity.RESULT_OK, IntentsUtil.accountRefreshed)
                                }
                                finish()
                            }
                        )
                    },
                    scaffoldState = scaffoldState
                ) { innerPadding ->
                    MemberActivityScreen(
                        userViewModel = userViewModel,
                        scaffoldState = scaffoldState,
                        showSnackBar = { msg ->
                            scope.launch {
                                scaffoldState.snackbarHostState.showSnackbar(msg)
                            }
                        },
                        modifier = Modifier.padding(innerPadding),
                        onRefreshed = {
                            refreshed = true
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        userViewModel.reloadAccount()
    }

    companion object {

        @JvmStatic
        fun start(context: Context?) {
            context?.startActivity(Intent(context, MemberActivity::class.java))
        }

        @JvmStatic
        fun launch(
            launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
            context: Context,
        ) {
            launcher.launch(
                Intent(context, MemberActivity::class.java)
            )
        }
    }
}
