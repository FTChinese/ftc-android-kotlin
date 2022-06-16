package com.ft.ftchinese.ui.main.splash

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.store.ServiceAcceptance
import com.ft.ftchinese.ui.util.ShareUtils
import com.ft.ftchinese.ui.webpage.WebTabScreen

/**
 * 3 case might trigger quiting splash screen:
 * - Splash scheme or image not found;
 * - User clicked skip button;
 * - User clicked image to see ad details.
 */
@Composable
fun SplashActivityScreen(
    onAgreement: () -> Unit,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    val agreement = remember {
        ServiceAcceptance.getInstance(context)
    }

    val splashState = rememberSplashState()

    LaunchedEffect(key1 = splashState.shouldExit) {
        if (splashState.shouldExit) {
            if (agreement.isAccepted()) {
                onNext()
            } else {
                onAgreement()
            }
        }
    }

    LaunchedEffect(key1 = Unit) {
        splashState.initLoading()
    }

    val (adLink, setAdLink) = remember {
        mutableStateOf("")
    }

    if (adLink.isBlank()) {
        SplashScreen(
            splash = splashState.splashShown,
            counter = splashState.currentTime,
            onClickCounter = {
                splashState.skip()
            },
            onClickImage = { url ->
                splashState.stopCounting()

                val uri = Uri.parse(url)

                if (ShareUtils.containWxMiniProgram(uri)) {
                    val params = ShareUtils.wxMiniProgramParams(uri)
                    if (params != null) {
                        ShareUtils
                            .createWxApi(context)
                            .sendReq(
                                ShareUtils.wxMiniProgramReq(params)
                            )

                        splashState.trackWxMini(params)
                    }

                    if (agreement.isAccepted()) {
                        onNext()
                    } else {
                        onAgreement()
                    }
                } else {
                    splashState.trackAdClicked()
                    setAdLink(url)
                }
            }
        )
    } else {
        WebTabScreen(
            url = adLink,
            openInBrowser = true,
            onClose = {
                if (agreement.isAccepted()) {
                    onNext()
                } else {
                    onAgreement()
                }
            }
        )
    }
}
