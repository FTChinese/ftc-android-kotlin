package com.ft.ftchinese.ui.main.splash

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.store.ServiceAcceptance
import com.ft.ftchinese.ui.util.ShareUtils
import com.ft.ftchinese.ui.web.WEB_PURCHASE_FLOW_TAG
import com.ft.ftchinese.ui.web.WvUrlEvent
import com.ft.ftchinese.ui.web.debugRouteName
import com.ft.ftchinese.ui.web.debugWebUrl
import com.ft.ftchinese.ui.web.launchCustomTabs
import com.ft.ftchinese.ui.web.rememberWebViewCallback
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
    val webViewCallback = rememberWebViewCallback(context = context)

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
                    val event = WvUrlEvent.fromUri(uri)
                    val campaignCode = when (event) {
                        is WvUrlEvent.FtaSubs -> event.ccode
                        is WvUrlEvent.Subscribe -> event.ccode
                        else -> uri.getQueryParameter("ccode")
                    }
                    val isDirectNativeCampaign = campaignCode.isNullOrBlank().not() && when (event) {
                        is WvUrlEvent.FtaSubs,
                        is WvUrlEvent.Subscribe,
                        is WvUrlEvent.Channel,
                        is WvUrlEvent.CorpPage,
                        is WvUrlEvent.Article -> true
                        else -> false
                    }

                    when {
                        // GAM wrappers must load in WebView first so their click is recorded.
                        event is WvUrlEvent.CampaignAd -> {
                            splashState.trackAdClicked()
                            Log.i(
                                WEB_PURCHASE_FLOW_TAG,
                                "splash_ad_campaign_wrapper url=${debugWebUrl(uri)}"
                            )
                            setAdLink(url)
                        }
                        isDirectNativeCampaign || event is WvUrlEvent.Subscribe -> {
                            splashState.trackAdClicked()
                            Log.i(
                                WEB_PURCHASE_FLOW_TAG,
                                "splash_ad_native route=${event.debugRouteName()} " +
                                    "ccode=${campaignCode.orEmpty()}"
                            )
                            // Put MainActivity underneath first, then place the native
                            // destination on top. Reversing this order would cover the
                            // subscription page with the home page.
                            onNext()
                            webViewCallback.onOverrideUrlLoading(event)
                        }
                        else -> {
                            splashState.trackAdClicked()
                            Log.i(
                                WEB_PURCHASE_FLOW_TAG,
                                "splash_ad_external url=${debugWebUrl(uri)}"
                            )
                            launchCustomTabs(context, uri)
                            if (agreement.isAccepted()) {
                                onNext()
                            } else {
                                onAgreement()
                            }
                        }
                    }
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
