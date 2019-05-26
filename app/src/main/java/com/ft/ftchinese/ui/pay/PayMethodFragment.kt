package com.ft.ftchinese.ui.pay

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders

import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedFragment
import com.ft.ftchinese.models.PayMethod
import kotlinx.android.synthetic.main.fragment_pay_method.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

private const val PERMISSIONS_REQUEST_CODE = 1002

@kotlinx.coroutines.ExperimentalCoroutinesApi
class PayMethodFragment : ScopedFragment(),
        AnkoLogger {

    private lateinit var viewModel: CheckOutViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pay_method, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProviders.of(this).get(CheckOutViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        alipay_btn.setOnClickListener {
            viewModel.selectPayMethod(PayMethod.ALIPAY)
        }

        wxpay_btn.setOnClickListener {
            viewModel.selectPayMethod(PayMethod.WXPAY)
        }

        requestPermission()
    }

    private fun requestPermission() {
        try {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {

                requestPermissions(
                        arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        PERMISSIONS_REQUEST_CODE
                )
            }
        } catch (e: IllegalStateException) {
            info(e)

            toast(R.string.permission_alipay_denied)

            allowAlipay(false)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                if (grantResults.isEmpty()) {
                    toast(R.string.permission_alipay_denied)

                    allowAlipay(false)
                    return
                }

                for (x in grantResults) {
                    if (x == PackageManager.PERMISSION_DENIED) {
                        toast(R.string.permission_alipay_denied)

                        allowAlipay(false)
                        return
                    }
                }

                toast(R.string.permission_alipay_granted)
            }
        }
    }

    private fun allowAlipay(value: Boolean) {
        alipay_btn.isEnabled = value
    }

    companion object {
        @JvmStatic
        fun newInstance() = PayMethodFragment()
    }
}
