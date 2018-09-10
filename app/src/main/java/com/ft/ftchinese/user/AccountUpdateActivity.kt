package com.ft.ftchinese.user

import android.content.Context
import android.content.Intent
import android.support.v4.app.Fragment
import com.ft.ftchinese.R
import com.ft.ftchinese.models.SessionManager

class AccountUpdateActivity : SingleFragmentActivity() {
    override fun createFragment(): Fragment {
        val itemId = intent.getIntExtra(ACCOUNT_ITEM_ID, 0)
        val user = SessionManager.getInstance(this).loadUser()
        return when (itemId) {
            AccountItem.ID_EMAIL -> {
                supportActionBar?.setTitle(R.string.title_change_email)
                EmailFragment.newInstance(user)
            }
            AccountItem.ID_USER_NAME -> {
                supportActionBar?.setTitle(R.string.title_change_username)
                UsernameFragment.newInstance(user)
            }
            AccountItem.ID_PASSWORD -> {
                supportActionBar?.setTitle(R.string.title_change_password)
                PasswordFragment.newInstance(user)
            }
            else -> {
                Fragment()
            }
        }
    }

    companion object {
        private const val ACCOUNT_ITEM_ID = "account_item_id"

        fun start(context: Context?, itemId: Int) {
            val intent = Intent(context, AccountUpdateActivity::class.java).apply {
                putExtra(ACCOUNT_ITEM_ID, itemId)
            }
            context?.startActivity(intent)
        }
    }
}