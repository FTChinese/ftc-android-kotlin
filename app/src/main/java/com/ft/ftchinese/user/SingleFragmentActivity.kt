package com.ft.ftchinese.user

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.ft.ftchinese.R
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.models.User
import kotlinx.android.synthetic.main.activity_fragment.*
import kotlinx.android.synthetic.main.simple_toolbar.*

/**
 * This interface must be implemented by activities that contain this
 * fragment to allow an interaction in this fragment to be communicated
 * to the activity and potentially other fragments contained in that
 * activity.
 *
 *
 * See the Android Training lesson [Communicating with Other Fragments]
 * (http://developer.android.com/training/basics/fragments/communicating.html)
 * for more information.
 */
internal interface OnFragmentInteractionListener {
    fun onProgress(show: Boolean)

    fun onUserSession(user: User)
}

abstract class SingleFragmentActivity : AppCompatActivity(), OnFragmentInteractionListener {

    protected abstract fun createFragment(): Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment)

        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        var fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (fragment == null) {
            fragment = createFragment()
            supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit()
        }
    }

    override fun onProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
    }

    override fun onUserSession(user: User) {
        val sessionManager = SessionManager.getInstance(applicationContext)
        sessionManager.saveUser(user)
    }
}

