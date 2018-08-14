package com.ft.ftchinese

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import kotlinx.android.synthetic.main.simple_toolbar.*

class SignupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {

            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }
    }

    companion object {

        fun start(context: Context?) {
            val intent = Intent(context, SignupActivity::class.java)

            context?.startActivity(intent)
        }

        fun startForResult(activity: Activity, requestCode: Int) {
            val intent = Intent(activity, SignupActivity::class.java)

            try {
                activity.startActivityForResult(intent, requestCode)

            } catch (e: ActivityNotFoundException) {
                Toast.makeText(activity, "Cannot launch login", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
