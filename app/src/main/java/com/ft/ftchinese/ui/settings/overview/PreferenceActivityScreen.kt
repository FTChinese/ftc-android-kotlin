package com.ft.ftchinese.ui.settings.overview

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.util.toast
import com.ft.ftchinese.ui.util.IntentsUtil
import com.ft.ftchinese.viewmodel.UserViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PreferenceActivityScreen(
    userViewModel: UserViewModel = viewModel(),
    scaffoldState: ScaffoldState,
    onLoggedOut: () -> Unit,
    onNavigateTo: (SettingScreen) -> Unit
) {
    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    val prefState = rememberPrefState(
        scaffoldState = scaffoldState,
        scope = scope,
    )

    LaunchedEffect(key1 = Unit) {
        prefState.calculateCacheSize()
        prefState.countReadArticles()
    }

    val rows = remember(
        context.resources,
        prefState.cacheSize,
        prefState.readEntries
    ) {
        SettingRow.build(
            context.resources,
            cacheSize = prefState.cacheSize,
            readCount = prefState.readEntries
        )
    }

    val bottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = false,
    )

    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetContent = {
            LogoutSheet {
                userViewModel.logout()
                scope.launch {
                    bottomSheetState.hide()
                }
                onLoggedOut()
            }
        },

    ) {
        PreferenceScreen(
            rows = rows,
            onClickRow = { rowId ->
                when (rowId) {
                    SettingScreen.ClearCache -> {
                        prefState.clearCache()
                    }
                    SettingScreen.ClearHistory -> {
                        prefState.truncateReadArticles()
                    }
                    SettingScreen.Feedback -> {
                        val ok = IntentsUtil.sendFeedbackEmail(context = context)
                        if (!ok) {
                            context.toast(R.string.prompt_no_email_app)
                        }
                    }
                    else -> {
                        onNavigateTo(rowId)
                    }
                }
            },
            isLoggedIn = userViewModel.isLoggedIn,
            onLogout = {
                scope.launch {
                    bottomSheetState.show()
                }
            }
        )
    }

}
