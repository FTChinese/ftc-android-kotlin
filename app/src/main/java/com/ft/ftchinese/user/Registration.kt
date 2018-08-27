package com.ft.ftchinese.user

import android.app.Activity
import android.content.Intent
import android.support.v4.app.Fragment

class Registration : SingleFragmentActivity() {
    override fun createFragment(): Fragment {
        return SignInOrUpFragment.newInstance(SignInOrUpFragment.USED_FOR_SIGN_UP)
    }

    companion object {
        fun startForResult(activity: Activity?, requestCode: Int) {
            val intent = Intent(activity, Registration::class.java)
            activity?.startActivityForResult(intent, requestCode)
        }
    }
}