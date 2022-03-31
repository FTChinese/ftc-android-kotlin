package com.ft.ftchinese.ui.subsactivity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.fetch.FetchUi
import com.ft.ftchinese.ui.components.ErrorDialog
import com.ft.ftchinese.ui.ftcpay.FtcPayScreen
import com.ft.ftchinese.ui.ftcpay.FtcPayViewModel
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
    val (loading, setLoading) = remember {
        mutableStateOf(false)
    }

    val fetchState = payViewModel.progressLiveData.observeAsState(FetchUi.Progress(false))

    when (val s = fetchState.value) {
        is FetchUi.Progress -> {
            setLoading(s.loading)
        }
        is FetchUi.ResMsg -> {
            setLoading(false)
            ErrorDialog(
                text = context.getString(s.strId)
            ) {
                payViewModel.clearPaymentError()
            }
        }
        is FetchUi.TextMsg -> {
            setLoading(false)
            ErrorDialog(
                text = s.text
            ) {
                payViewModel.clearPaymentError()
            }
        }
    }

    val account = userViewModel.account
    if (account == null) {
        showSnackBar("Not logged in")
        return
    }

    if (priceId.isNullOrBlank()) {
        showSnackBar("Price id not passed in")
        return
    }

    val item = pwViewModel.ftcCheckoutItem(
        priceId = priceId,
        m = account.membership.normalize(),
    )

    if (item != null) {

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
    } else {
        showSnackBar("Empty shopping cart")
    }
}
