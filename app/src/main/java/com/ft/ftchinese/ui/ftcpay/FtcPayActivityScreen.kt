package com.ft.ftchinese.ui.ftcpay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.ui.components.ErrorDialog
import com.ft.ftchinese.ui.components.ToastMessage
import com.ft.ftchinese.ui.paywall.PaywallViewModel
import com.ft.ftchinese.viewmodel.UserViewModel
import com.tencent.mm.opensdk.constants.Build
import com.tencent.mm.opensdk.openapi.IWXAPI

@Composable
fun FtcPayActivityScreen(
    userViewModel: UserViewModel = viewModel(),
    payViewModel: FtcPayViewModel,
    pwViewModel: PaywallViewModel,
    priceId: String?,
    wxApi: IWXAPI,
    showSnackBar: (String) -> Unit,
) {
    val context = LocalContext.current

    val loading by payViewModel.progressLiveData.observeAsState(false)
    val toastState by payViewModel.toastLiveData.observeAsState(null)

    toastState?.let {
        val msg = when (it) {
            is ToastMessage.Resource -> {
                context.getString(it.id)
            }
            is ToastMessage.Text -> {
                it.text
            }
        }

        ErrorDialog(
            text = msg
        ) {
            payViewModel.resetToast()
        }
    }

    val accountState = userViewModel.accountLiveData.observeAsState()
    val account = accountState.value
    if (account == null) {
        showSnackBar("Not logged in")
        return
    }

    if (priceId.isNullOrBlank()) {
        showSnackBar("Price id not passed in")
        return
    }

    pwViewModel.ftcCheckoutItem(
        priceId = priceId,
        m = account.membership.normalize(),
    )?.let { item ->
        FtcPayScreen(
            cartItem = item,
            loading = loading,
            onClickPay = { payMethod ->
                if (payMethod == PayMethod.WXPAY && wxApi.wxAppSupportAPI < Build.PAY_SUPPORTED_SDK_INT) {
                    showSnackBar(context.getString(R.string.wxpay_not_supported))
                    return@FtcPayScreen
                }

                payViewModel.createOrder(
                    account = account,
                    cartItem = item,
                    payMethod = payMethod
                )
            }
        )
    }
}
