package com.ft.ftchinese.user

import android.app.Activity
import android.content.Intent
import android.support.v4.app.Fragment
import com.ft.ftchinese.util.RequestCode

class SignInActivity : SingleFragmentActivity() {
    override fun createFragment(): Fragment {

        return SignInOrUpFragment.newInstanceSignIn()
    }

    companion object {

        fun start(activity: Activity?) {
            val intent = Intent(activity, SignInActivity::class.java)

            activity?.startActivityForResult(intent, RequestCode.SIGN_IN)
        }
    }
}