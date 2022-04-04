package com.ft.ftchinese.ui.settings

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.*
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor
import com.ft.ftchinese.ui.theme.OTheme
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging

class FCMActivity : AppCompatActivity() {

    private var errorDialog: Dialog? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OTheme {

                val state = remember {
                    FcmState()
                }

                Scaffold(
                    topBar = {
                        Toolbar(
                            heading = stringResource(id = R.string.fcm_setting_title),
                            onBack = { finish() }
                        )
                    }
                ) {
                    FcmScreen(
                        state = state,
                        onCheckPlayService = {
                            state.inProgress = true
                            state.clear()

                            val available = checkPlayServices()
                            state.playAvailable = available

                            retrieveRegistrationToken {
                                state.inProgress = false
                                state.tokenRetrievable = it.isSuccessful

                                // val token = it.result?.token
                            }
                        }
                    )
                }
            }
        }
    }



    private fun checkPlayServices(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)

        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                if (errorDialog == null) {
                    errorDialog = googleApiAvailability.getErrorDialog(this, resultCode, 2404)
                    errorDialog?.setCancelable(false)
                }

                if (errorDialog?.isShowing == false) {
                    errorDialog?.show()
                }
            }
        }

        return resultCode == ConnectionResult.SUCCESS
    }

    private fun retrieveRegistrationToken(listener: OnCompleteListener<String>) {
        FirebaseMessaging
            .getInstance()
            .token
            .addOnCompleteListener(listener)
    }

    companion object {
        @JvmStatic
        fun start(context: Context?) {
            context?.startActivity(Intent(context, FCMActivity::class.java))
        }
    }
}

private fun launchNotificationSetting(context: Context) {
    val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID)
        putExtra(Settings.EXTRA_CHANNEL_ID, context.getString(R.string.news_notification_channel_id))
    }

    context.startActivity(intent)
}

class FcmState {
    var inProgress by mutableStateOf(false)

    var playAvailable by mutableStateOf<Boolean?>(null)

    var tokenRetrievable by mutableStateOf<Boolean?>(null)

    fun clear() {
        playAvailable = null
        tokenRetrievable = null
    }
}

@Composable
private fun FcmScreen(
    state: FcmState,
    onCheckPlayService: () -> Unit
) {
    val context = LocalContext.current

    FcmBody(
        loading = state.inProgress,
        isPlayAvailable = state.playAvailable,
        isTokenRetrievable = state.tokenRetrievable,
        onSetting = {
            launchNotificationSetting(context)
        },
        onCheck = onCheckPlayService,
    )
}

@Composable
private fun FcmBody(
    loading: Boolean,
    isPlayAvailable: Boolean? = null,
    isTokenRetrievable: Boolean? = null,
    onSetting: () -> Unit,
    onCheck: () -> Unit,
) {

    ProgressLayout(
        loading = loading
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(id = R.string.fcm_check_guide),
                    modifier = Modifier.padding(Dimens.dp16),
                    style = MaterialTheme.typography.body2,
                    color = OColor.black60,
                )

                ClickableRow(
                    onClick = onSetting,
                    modifier = Modifier
                        .background(OColor.black5)
                        .padding(Dimens.dp16)
                ) {
                    Text(
                        text = stringResource(id = R.string.channel_setting_news),
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.weight(1f)
                    )
                }

                Column(
                    modifier = Modifier.padding(Dimens.dp16)
                ) {
                    isPlayAvailable?.let {
                        PlayServiceStatus(available = it)
                    }

                    Spacer(modifier = Modifier.height(Dimens.dp16))

                    isTokenRetrievable?.let {
                        TokenStatus(retrievable = it)
                    }
                }
            }

            PrimaryButton(
                onClick = onCheck,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.dp16),
                enabled = !loading,
            ) {
                Text(text = stringResource(id = R.string.fcm_start_checking))
            }
        }
    }
}

@Composable
fun PlayServiceStatus(
    available: Boolean
) {
    if (available) {
        IconTextItem(
            icon = painterResource(id = R.drawable.ic_done_claret_24dp),
            text = stringResource(id = R.string.play_service_available),
            iconTint = OColor.claret
        )
    } else {
        IconTextItem(
            icon = painterResource(id = R.drawable.ic_error_outline_claret_24dp),
            text = stringResource(id = R.string.play_service_not_available),
            iconTint = OColor.claret
        )
    }
}

@Composable
private fun TokenStatus(
    retrievable: Boolean,
) {
    if (retrievable) {
        IconTextItem(
            icon = painterResource(id = R.drawable.ic_done_claret_24dp),
            text = stringResource(R.string.fcm_accessible),
            iconTint = OColor.claret,
        )
    } else {
        IconTextItem(
            icon = painterResource(id = R.drawable.ic_error_outline_claret_24dp),
            text = stringResource(R.string.fcm_inaccessible),
            iconTint = OColor.claret
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewFcmBody() {
    FcmBody(
        loading = true,
        isPlayAvailable = true,
        isTokenRetrievable = true,
        onSetting = {},
        onCheck = {}
    )
}
