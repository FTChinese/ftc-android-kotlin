package com.ft.ftchinese.ui.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedAppActivity
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger

@kotlinx.coroutines.ExperimentalCoroutinesApi
class WxInfoActivity : ScopedAppActivity(), AnkoLogger {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fragment_single)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        supportFragmentManager.beginTransaction()
                .replace(R.id.single_frag_holder, WxFragment.newInstance())
                .commit()
    }

    companion object {
        fun start(context: Context?) {
            context?.startActivity(
                    Intent(context, WxInfoActivity::class.java)
            )
        }
    }
}
