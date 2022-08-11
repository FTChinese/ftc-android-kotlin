package com.ft.ftchinese.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import android.provider.Settings
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.Edition
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.ui.formatter.FormatHelper
import kotlinx.parcelize.Parcelize

fun Intent.putParcelableExtra(key: String, value: Parcelable) {
    putExtra(key, value)
}

fun composeDeletionEmail(
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

object IntentsUtil {
    private fun emailIntentCustomerService(
        title: String,
        body: String? = null
    ): Intent {
        return Intent(Intent.ACTION_SENDTO).apply {
            type = "text/plain"
            // According to Android docs, use `Uri.parse("mailto:")` restrict the intent for mail apps.
            // However, Netease Mail Master does not follow the `Intent.EXTRA_EMAIL` standards, thus we have to duplicate the to email here.
            data = Uri.parse("mailto:subscriber.service@ftchinese.com")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("subscriber.service@ftchinese.com"))
            putExtra(Intent.EXTRA_SUBJECT, title)
            if (body != null) {
                putExtra(Intent.EXTRA_TEXT, body)
            }
        }
    }

    private fun emailIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_SENDTO).apply {
            data = uri
        }
    }

    private fun browserIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = uri
        }
    }

    private fun emailIntentFeedback(): Intent {
        return Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:ftchinese.feedback@gmail.com")
            putExtra(Intent.EXTRA_SUBJECT, "Feedback from FTC Android App")
        }
    }

    private fun sendEmail(context: Context, intent: Intent): Boolean {
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            true
        } else {
            false
        }
    }

    fun openInBrowser(context: Context, uri: Uri): Boolean {
        val intent = browserIntent(uri)
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            true
        } else {
            false
        }
    }

    fun sendEmail(context: Context, uri: Uri): Boolean {
        return sendEmail(context, emailIntent(uri))
    }

    fun sendFeedbackEmail(context: Context): Boolean {
        return sendEmail(context, emailIntentFeedback())
    }

    fun sendCustomerServiceEmail(context: Context): Boolean {
        return sendEmail(
            context,
            emailIntentCustomerService(
                title = "FT中文网会员订阅"
            )
        )
    }

    fun sendDeleteAccountEmail(
        context: Context,
        account: Account,
    ): Boolean {
        return sendEmail(
            context = context,
            emailIntentCustomerService(
                title = context.getString(R.string.subject_delete_account_valid_subs),
                body = composeDeletionEmail(context, account)
            )
        )
    }

    fun openSetting(context: Context) {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.also { intent ->
            context.startActivity(intent)
        }
    }

    private const val EXTRA_ACCOUNT_ACTION = "com.ft.ftchinese.EXTRA_ACCOUNT_ACTION"
    val signedIn = Intent().apply {
        putParcelableExtra(EXTRA_ACCOUNT_ACTION, AccountAction.SignedIn)
    }
    val loggedOut = Intent().apply {
        putParcelableExtra(EXTRA_ACCOUNT_ACTION, AccountAction.LoggedOut)
    }
    val accountDeleted = Intent().apply {
        putParcelableExtra(EXTRA_ACCOUNT_ACTION, AccountAction.Deleted)
    }
    val accountRefreshed = Intent().apply {
        putParcelableExtra(EXTRA_ACCOUNT_ACTION, AccountAction.Refreshed)
    }

    fun getAccountAction(intent: Intent): AccountAction? {
        return intent.getParcelableExtra(EXTRA_ACCOUNT_ACTION)
    }
}


@Parcelize
enum class AccountAction : Parcelable {
    SignedIn,
    LoggedOut,
    Deleted,
    Refreshed;
}
