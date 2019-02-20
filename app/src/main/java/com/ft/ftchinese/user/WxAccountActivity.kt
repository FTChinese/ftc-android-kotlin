package com.ft.ftchinese.user

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.ft.ftchinese.R
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*

class WxAccountActivity : AppCompatActivity(),
        OnAccountInteractionListener{

    override fun onProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
    }

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