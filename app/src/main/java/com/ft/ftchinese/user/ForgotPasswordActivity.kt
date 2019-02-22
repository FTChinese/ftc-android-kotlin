package com.ft.ftchinese.user

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.ft.ftchinese.R
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger

class ForgotPasswordActivity : AppCompatActivity(),
        OnCredentialsListener,
        AnkoLogger {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        val email = intent.getStringExtra(ARG_EMAIL) ?: return

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, ForgotPasswordFragment.newInstance(email))
                .commit()
    }

    override fun onProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
    }

    override fun onLogIn(email: String) {

    }

    override fun onSignUp(email: String) {

    }

    override fun onLoadAccount(userId: String) {

    }

    companion object {
        fun start(context: Context?, email: String) {
            val intent = Intent(context, ForgotPasswordActivity::class.java).apply {
                putExtra(ARG_EMAIL, email)
            }

            context?.startActivity(intent)
        }
    }
}

