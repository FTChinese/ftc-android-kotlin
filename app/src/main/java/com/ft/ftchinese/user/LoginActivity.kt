package com.ft.ftchinese.user

import android.app.Activity
import android.content.Intent
import android.support.v4.app.Fragment


class LoginActivity : SingleFragmentActivity() {

    override fun createFragment(): Fragment {
        return SignInOrUpFragment.newInstance(SignInOrUpFragment.USED_FOR_LOGIN)
    }

    companion object {
        /**
         * When launching LoginActivity, you usually should notify the caller activity login result.
         * @param activity The activity that launched this activity.
         * @param requestCode returned to the calling activity's onActivityResult()
         */
        fun startForResult(activity: Activity?, requestCode: Int) {
            val intent = Intent(activity, LoginActivity::class.java)

            activity?.startActivityForResult(intent, requestCode)
        }
    }
}