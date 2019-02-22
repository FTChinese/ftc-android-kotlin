package com.ft.ftchinese.user

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ft.ftchinese.R
import kotlinx.android.synthetic.main.fragment_customer_service.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.toast

class CustomerServiceFragment : Fragment(), AnkoLogger {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_customer_service, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        email_tv.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, "subscriber.service@ftchinese.com")
                putExtra(Intent.EXTRA_SUBJECT, "FT中文网会员订阅")
            }

            val pm = activity?.packageManager

            if (pm == null) {
                toast("Cannot send email now!")
            }

            if (intent.resolveActivity(pm) != null) {
                startActivity(intent)
            } else {
                toast(R.string.prompt_no_email_app)
            }
        }
    }

    companion object {
        fun newInstance() = CustomerServiceFragment()
    }
}