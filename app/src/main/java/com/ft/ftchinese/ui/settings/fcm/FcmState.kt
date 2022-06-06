package com.ft.ftchinese.ui.settings.fcm

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging

class FcmState(
    private val context: Context,
) {

    var progress by mutableStateOf(false)
        private set

    var messages by mutableStateOf(listOf<IconTextRow>())
        private set

    fun checkFcm() {
        val msgBuilder = FcmMessageBuilder()
        progress = true

        val playAvailable = checkPlayServices()
        messages = msgBuilder
            .addPlayService(playAvailable)
            .build()

        retrieveRegistrationToken {
            messages = msgBuilder
                .addTokenRetrievable(it.isSuccessful)
                .build()

            progress = false
        }
    }

    private fun checkPlayServices(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)

        return resultCode == ConnectionResult.SUCCESS
    }

    private fun retrieveRegistrationToken(listener: OnCompleteListener<String>) {
        FirebaseMessaging
            .getInstance()
            .token
            .addOnCompleteListener(listener)
    }
}

@Composable
fun rememberFcmState(
    context: Context = LocalContext.current
) = remember {
    FcmState(context)
}
