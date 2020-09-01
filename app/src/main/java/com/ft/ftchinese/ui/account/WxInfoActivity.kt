package com.ft.ftchinese.ui.account

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityAccountBinding
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.util.RequestCode
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger

@kotlinx.coroutines.ExperimentalCoroutinesApi
class WxInfoActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var binding: ActivityAccountBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Here we reused the activity_account.
        binding = DataBindingUtil.setContentView(this, R.layout.activity_account)
        setSupportActionBar(toolbar)

        sessionManager = SessionManager.getInstance(this)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        val account = sessionManager.loadAccount()
        supportFragmentManager.commit {
            when {
                account?.isLinked == true -> {
                    replace(R.id.frag_account, WxInfoFragment.newInstance())
                }
                account?.isFtcOnly == true -> {
                    replace(R.id.frag_account, WxAuthorizeFragment.newInstance())
                }
            }

        }
    }

    /**
     * Received results from [UnlinkActivity] with
     * RequestCode.Unlink.
     * If [WxAuthorizeFragment] is used inside this activity,
     * user starts linking wechat to FTC account, ideally
     * this activity should receive a RequestCode.Link
     * message. But I'm not sure whether this works or not.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) {
            return
        }

        /**
         * Chain of unwrapping:
         * [AccountActivity] -> [WxInfoActivity] -> [UnlinkActivity]
         */
        if (requestCode == RequestCode.UNLINK) {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    companion object {

//        fun startForResult(activity: Activity?, requestCode: Int) {
//            activity?.startActivityForResult(
//                    Intent(activity, WxInfoActivity::class.java),
//                    requestCode
//            )
//        }

        fun start(context: Context) {
            context.startActivity(Intent(context, WxInfoActivity::class.java))
        }
    }
}
