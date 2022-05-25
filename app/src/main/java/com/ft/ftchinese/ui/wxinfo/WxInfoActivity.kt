package com.ft.ftchinese.ui.wxinfo

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.theme.OTheme
import com.ft.ftchinese.viewmodel.UserViewModel

class WxInfoActivity : ComponentActivity() {
    private lateinit var userViewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]

        setContent {
            OTheme {
                ScreenLayout(
                    userViewModel = userViewModel,
                    onBack =  {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        userViewModel.reloadAccount()
    }

    companion object {

        @JvmStatic
        fun newIntent(context: Context) = Intent(context, WxInfoActivity::class.java)
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
private fun ScreenLayout(
    userViewModel: UserViewModel = viewModel(),
    onBack: () -> Unit,
) {
    val scaffoldState = rememberScaffoldState()


    Scaffold(
        topBar = {
            Toolbar(
                heading = stringResource(id = R.string.title_wx_account),
                onBack = onBack
            )
        },
        scaffoldState = scaffoldState
    ) {
        WxInfoActivityScreen(
            userViewModel = userViewModel,
            scaffold = scaffoldState,
        )
    }
}
