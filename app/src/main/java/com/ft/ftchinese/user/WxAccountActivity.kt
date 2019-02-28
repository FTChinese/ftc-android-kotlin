package com.ft.ftchinese.user

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.ft.ftchinese.R
import kotlinx.android.synthetic.main.simple_toolbar.*

/**
 * This is only used to present wechat account information
 * if it is bound to a ftc account, and user clicked the
 * `wechat_container` button in AccountActivity.
 */
class WxAccountActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, WxAccountFragment.newInstance())
                .commit()
    }

    companion object {
        fun start(context: Context?) {
            context?.startActivity(
                    Intent(context, WxAccountActivity::class.java))
        }
    }
}