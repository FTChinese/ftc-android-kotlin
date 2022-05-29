package com.ft.ftchinese.ui.account.delete

import android.content.Context
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.Edition
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.request.AccountDropped
import com.ft.ftchinese.model.request.EmailPasswordParams
import com.ft.ftchinese.ui.base.IntentsUtil
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.formatter.FormatHelper
import com.ft.ftchinese.viewmodel.UserViewModel

@Composable
fun DeleteAccountActivityScreen(
    userViewModel: UserViewModel,
    scaffoldState: ScaffoldState,
    onDeleted: () -> Unit,
) {
    val context = LocalContext.current
    val accountState = userViewModel.accountLiveData.observeAsState()
    val account = accountState.value

    val uiState = rememberDeleteAccountState(
        scaffoldState = scaffoldState
    )

    if (account == null) {
        return
    }

    uiState.dropped?.let {
        when (it) {
            AccountDropped.Success -> {
                uiState.resetDropped()
                onDeleted()
            }
            AccountDropped.SubsExists -> {
                AlertDeleteDenied(
                    onDismiss = uiState::resetDropped,
                    onConfirm = {
                        val ok = sendEmail(
                            context = context,
                            account = account
                        )
                        if (!ok) {
                            uiState.showSnackBar(R.string.prompt_no_email_app)
                        }
                        uiState.resetDropped()
                    }
                )
            }
        }
    }

    ProgressLayout(
        loading = uiState.progress.value
    ) {
        DeleteAccountScreen(
            loading = uiState.progress.value,
            onVerify = {
                uiState.drop(
                    ftcId = account.id,
                    params = EmailPasswordParams(
                        email = account.email,
                        password = it,
                    )
                )
            }
        )
    }
}

private fun sendEmail(
    context: Context,
    account: Account,
): Boolean {
    val intent = IntentsUtil.emailCustomerService(
        title = context.getString(R.string.subject_delete_account_valid_subs),
        body = composeDeletionEmail(context, account)
    )

    return if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
        true
    } else {
        false
    }
}

private fun composeDeletionEmail(
    context: Context,
    a: Account
): String? {
    val emailOrMobile = if (a.isMobileEmail) {
        a.mobile
    } else {
        a.email
    }

    if (a.membership.tier == null || a.membership.cycle == null) {
        return null
    }

    val edition = FormatHelper.formatEdition(
        context,
        Edition(
            tier = a.membership.tier,
            cycle = a.membership.cycle,
        )
    )

    return "FT中文网，\n请删除我的账号 $emailOrMobile。\n我的账号已经购买了FT中文网付费订阅服务 $edition，到期时间 ${a.membership.localizeExpireDate()}。我已知悉删除账号的同时将删除我的订阅信息。"
}
