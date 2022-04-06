package com.ft.ftchinese.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.ClickableRow
import com.ft.ftchinese.ui.components.ToastMessage
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor
import com.ft.ftchinese.ui.theme.OTheme
import org.jetbrains.anko.toast

// Reference: https://developer.android.com/guide/topics/ui/settings
class SettingsActivity : ComponentActivity() {

    private lateinit var settingsViewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        settingsViewModel.toastMessage.observe(this) {
            when (it) {
                is ToastMessage.Resource -> toast(it.id)
                is ToastMessage.Text -> toast(it.text)
            }
        }

        setContent {
            OTheme {
                Scaffold(
                    topBar = {
                        Toolbar(
                            heading = stringResource(id = R.string.action_settings),
                            onBack = { finish() }
                        )
                    }
                ) {
                    PreferenceScreen(
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, SettingsActivity::class.java))
        }
    }
}

enum class PrefId {
    ClearCache,
    ClearHistory,
    Notification,
    CheckVersion,
}

@Composable
fun PreferenceScreen(
    settingsViewModel: SettingsViewModel,
) {
    val context = LocalContext.current
    val cacheSize by settingsViewModel.cacheSizeLiveData.observeAsState()
    val readCount by settingsViewModel.articlesReadLiveData.observeAsState()

    LaunchedEffect(key1 = Unit) {
        settingsViewModel.calculateCacheSize()
        settingsViewModel.countReadArticles()
    }

    PreferenceBody(
        cacheSize = cacheSize,
        readCount = readCount,
        onClickRow = { rowId ->
            when (rowId) {
                PrefId.ClearCache -> {
                    settingsViewModel.clearCache()
                }
                PrefId.ClearHistory -> {
                    settingsViewModel.truncateReadArticles()
                }
                PrefId.Notification -> {
                    FCMActivity.start(context)
                }
                PrefId.CheckVersion -> {
                    UpdateAppActivity.start(context)
                }
            }
        }
    )
}

@Composable
fun PreferenceBody(
    cacheSize: String?,
    readCount: Int?,
    onClickRow: (PrefId) -> Unit
) {
    Column {
        PreferenceItem(
            title = stringResource(id = R.string.pref_clear_cache),
            summary = cacheSize ?: "0 KiB",
            leadIcon = painterResource(id = R.drawable.ic_clear_24dp),
            trailIcon = null,
            onClick = {
                onClickRow(PrefId.ClearCache)
            }
        )

        PreferenceItem(
            title = stringResource(id = R.string.pref_clear_history),
            summary = readCount?.let {
                stringResource(R.string.summary_articles_read, it)
            } ?: "",
            leadIcon = painterResource(id = R.drawable.ic_delete_forever_black_24dp),
            trailIcon = null,
            onClick = {
                onClickRow(PrefId.ClearHistory)
            }
        )

        PreferenceItem(
            title = stringResource(id = R.string.fcm_pref),
            summary = stringResource(id = R.string.fcm_summary),
            leadIcon = painterResource(id = R.drawable.ic_notifications_black_24dp),
            trailIcon = painterResource(id = R.drawable.ic_keyboard_arrow_right_gray_24dp),
            onClick = {
                onClickRow(PrefId.Notification)
            }
        )

        PreferenceItem(
            title = stringResource(id = R.string.pref_check_new_version),
            summary = stringResource(R.string.current_version, BuildConfig.VERSION_NAME),
            leadIcon = painterResource(id = R.drawable.ic_update_black_24dp),
            trailIcon = painterResource(id = R.drawable.ic_keyboard_arrow_right_gray_24dp),
            onClick = {
                onClickRow(PrefId.CheckVersion)
            },
        )
    }
}

@Composable
fun PreferenceItem(
    title: String,
    summary: String,
    leadIcon: Painter,
    trailIcon: Painter?,
    onClick: () -> Unit,
) {
    ClickableRow(
        onClick = onClick,
        trailIcon = trailIcon,
    ) {
        Icon(
            painter = leadIcon,
            contentDescription = title
        )

        Column(
            modifier = Modifier
                .padding(
                    start = Dimens.dp8,
                    end = Dimens.dp8
                )
                .weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.padding(bottom = Dimens.dp8)
            )
            Text(
                text = summary,
                color = OColor.black60,
                style = MaterialTheme.typography.body2
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPreferenceBody() {
    PreferenceBody(
        cacheSize = "11.2kb",
        readCount = 5,
        onClickRow = {}
    )
}
