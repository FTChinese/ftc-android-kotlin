package com.ft.ftchinese.user

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ft.ftchinese.R

class ChangeUsernameActivity : SingleFragmentActivity() {
    override fun createFragment(): Fragment {
        return ChangeUsernameFragment.newInstance()
    }
}

internal class ChangeUsernameFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_change_username, container, false)
    }

    companion object {
        fun newInstance(): ChangePasswordFragment {
            return ChangePasswordFragment()
        }
    }
}