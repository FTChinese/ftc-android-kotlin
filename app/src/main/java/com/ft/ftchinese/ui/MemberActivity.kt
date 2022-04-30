package com.ft.ftchinese.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.member.MemberActivityScreen
import com.ft.ftchinese.ui.member.MembershipViewModel
import com.ft.ftchinese.ui.theme.OTheme
import com.ft.ftchinese.util.RequestCode
import kotlinx.coroutines.launch

class MemberActivity : ComponentActivity() {
    private lateinit var membershipViewModel: MembershipViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        membershipViewModel = ViewModelProvider(this)[MembershipViewModel::class.java]

        setContent {
            OTheme {
                val scaffoldState = rememberScaffoldState()
                val scope = rememberCoroutineScope()

                Scaffold(
                    topBar = {
                        Toolbar(
                            heading = stringResource(id = R.string.title_my_subs),
                            onBack = {
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        )
                    },
                    scaffoldState = scaffoldState
                ) { innerPadding ->
                    MemberActivityScreen(
                        memberViewModel = membershipViewModel,
                        showSnackBar = { msg ->
                            scope.launch {
                                scaffoldState.snackbarHostState.showSnackbar(msg)
                            }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    companion object {

        @JvmStatic
        fun start(context: Context?) {
            context?.startActivity(Intent(context, MemberActivity::class.java))
        }

        @JvmStatic
        fun startForResult(activity: Activity?) {
            activity?.startActivityForResult(
                Intent(activity, MemberActivity::class.java),
                RequestCode.MEMBER_REFRESHED
            )
        }

        // Intent in place of old startActivityForResult.
        @JvmStatic
        fun startIntent(context: Context) = Intent(context, MemberActivity::class.java)
    }
}
