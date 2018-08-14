package com.ft.ftchinese

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.simple_toolbar.*

class CreateAccountActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            // This should be conditionally true depending on from where this activity is launched.
            setDisplayHomeAsUpEnabled(true)
            setTitle(R.string.title_create_account)
        }
    }
}
