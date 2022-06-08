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
import com.ft.ftchinese.repository.ApiConfig
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.viewmodel.UserViewModel
import com.tencent.mm.opensdk.constants.Build
import com.tencent.mm.opensdk.openapi.WXAPIFactory

@Composable
fun FtcPayActivityScreen(
    userViewModel: UserViewModel = viewModel(),
    payViewModel: FtcPayViewModel,
    scaffoldState: ScaffoldState,
    priceId: String?,
    onAliPay: (AliPayIntent) -> Unit,
    onSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val wxApi = remember {
        WXAPIFactory.createWXAPI(
            context,
            BuildConfig.WX_SUBS_APPID
        )
    }

    val aliPayState = payViewModel.aliPayResult.observeAsState()
    val aliPayResult = aliPayState.value

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

    val apiConfig = remember {
        ApiConfig.ofSubs(account.isTest)
    }

    LaunchedEffect(key1 = Unit) {
        ftcPayState.loadFtcCheckoutItem(
            priceId = priceId,
            membership = account.membership
        )
    }

    // After order created
    LaunchedEffect(key1 = ftcPayState.paymentIntent) {
        ftcPayState.paymentIntent?.let {
            when (it) {
                is OrderResult.WxPay -> {
                    val params = it.intent.params.app
                    if (params == null) {
                        ftcPayState.showSnackBar("Wx order missing required parameters")
                        return@LaunchedEffect
                    }

                    wxApi.sendReq(params.buildReq())
                }
                is OrderResult.AliPay -> {
                    onAliPay(it.intent)
                }
            }
        }
    }

    // After alipay sdk is called
    LaunchedEffect(key1 = aliPayResult) {
        aliPayResult?.let {
            ftcPayState.handleAliPayResult(
                account = account,
                aliPayIntent = it.intent,
                payResult = it.result
            )
            payViewModel.clear()
        }
    }

    // After alipay is confirmed.
    LaunchedEffect(key1 = ftcPayState.membershipUpdated) {
        ftcPayState.membershipUpdated?.let {
            userViewModel.saveMembership(it)
            onSuccess()
        }
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
                        payMethod = payMethod,
                        item = item,
                    )
                },
                mode = apiConfig.mode
            )
        }
    }
}


