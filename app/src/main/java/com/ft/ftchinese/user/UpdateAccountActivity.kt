package com.ft.ftchinese.user

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.ft.ftchinese.R
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*

class UpdateAccountActivity : AppCompatActivity(),
        OnAccountInteractionListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        val fragType = intent.getIntExtra(TARGET_FRAG, 0)

        val fragment: Fragment = when (fragType) {
            FRAG_EMAIL -> {
                supportActionBar?.setTitle(R.string.title_change_email)
                UpdateEmailFragment.newInstance()
            }
            FRAG_USER_NAME -> {
                supportActionBar?.setTitle(R.string.title_change_username)
                UpdateNameFragment.newInstance()
            }
            FRAG_PASSWORD -> {
                supportActionBar?.setTitle(R.string.title_change_password)
                UpdatePasswordFragment.newInstance()
            }
            else -> {
                return
            }
        }

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
    }

    override fun onProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
    }

    companion object {
        private const val TARGET_FRAG = "extra_target_fragment"
        private const val FRAG_EMAIL = 1
        private const val FRAG_USER_NAME = 2
        private const val FRAG_PASSWORD = 3

        fun startForEmail(context: Context?) {
            context?.startActivity(
                    Intent(context, UpdateAccountActivity::class.java).apply {
                        putExtra(TARGET_FRAG, FRAG_EMAIL)
                    }
            )
        }

        fun startForUserName(context: Context?) {
            context?.startActivity(
                    Intent(context, UpdateAccountActivity::class.java).apply {
                        putExtra(TARGET_FRAG, FRAG_USER_NAME)
                    }
            )
        }

        fun startForPassword(context: Context?) {
            context?.startActivity(
                    Intent(context, UpdateAccountActivity::class.java).apply {
                        putExtra(TARGET_FRAG, FRAG_PASSWORD)
                    }
            )
        }
    }
}