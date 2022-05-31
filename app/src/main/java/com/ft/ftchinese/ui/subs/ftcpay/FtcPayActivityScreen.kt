package com.ft.ftchinese.ui.subs.ftcpay

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.ftcsubs.AliPayIntent
import com.ft.ftchinese.model.ftcsubs.WxPayIntent
import com.ft.ftchinese.store.PayIntentStore
import com.ft.ftchinese.tracking.BeginCheckoutParams
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.viewmodel.UserViewModel
import com.tencent.mm.opensdk.constants.Build
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory

@Composable
fun FtcPayActivityScreen(
    userViewModel: UserViewModel = viewModel(),
    scaffoldState: ScaffoldState,
    priceId: String?,
    onAliPay: (AliPayIntent) -> Unit
) {
    val context = LocalContext.current
    val wxApi = remember {
        WXAPIFactory.createWXAPI(
            context,
            BuildConfig.WX_SUBS_APPID
        )
    }
    val tracker = remember {
        StatsTracker.getInstance(context)
    }
    val payIntentStore = remember {
        PayIntentStore.getInstance(context)
    }

    val accountState = userViewModel.accountLiveData.observeAsState()
    val account = accountState.value

    val ftcPayState = rememberFtcPaySate(
        scaffoldState = scaffoldState,
    )

    if (account == null) {
        ftcPayState.showSnackBar("Not logged in")
        return
    }

    if (priceId.isNullOrBlank()) {
        ftcPayState.showSnackBar("Price id not passed in")
        return
    }

    LaunchedEffect(key1 = ftcPayState.paymentIntent) {
        ftcPayState.paymentIntent?.let {
            when (it) {
                is OrderResult.WxPay -> {
                    payIntentStore.save(it.intent.toPayIntent())
                    launchWxPay(
                        wxApi = wxApi,
                        wxPayIntent = it.intent
                    )
                }
                is OrderResult.AliPay -> {
                    onAliPay(it.intent)
                }
            }
        }
    }

    LaunchedEffect(key1 = Unit) {
        ftcPayState.loadFtcCheckoutItem(
            priceId = priceId,
            membership = account.membership
        )
    }

    ProgressLayout(
        loading = ftcPayState.progress.value
    ) {
        ftcPayState.cartItem?.let { item ->
            FtcPayScreen(
                cartItem = item,
                loading = ftcPayState.progress.value,
                onClickPay = { payMethod ->
                    if (payMethod == PayMethod.WXPAY && wxApi.wxAppSupportAPI < Build.PAY_SUPPORTED_SDK_INT) {
                        ftcPayState.showSnackBar(context.getString(R.string.wxpay_not_supported))
                        return@FtcPayScreen
                    }

                    ftcPayState.createOrder(
                        account = account,
                        payMethod = payMethod
                    )

                    tracker.beginCheckOut(
                        BeginCheckoutParams.ofFtc(
                            item = item,
                            method = payMethod
                        )
                    )
                }
            )
        }
    }
}

private fun launchWxPay(
    wxApi: IWXAPI,
    wxPayIntent: WxPayIntent
): Boolean {
    val params = wxPayIntent.params.app ?: return false

    return wxApi.sendReq(params.buildReq())
}

