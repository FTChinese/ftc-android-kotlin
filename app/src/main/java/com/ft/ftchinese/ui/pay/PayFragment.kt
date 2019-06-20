package com.ft.ftchinese.ui.pay

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer

import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedFragment
import com.ft.ftchinese.model.PayMethod
import kotlinx.android.synthetic.main.pay_fragment.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class PayFragment : ScopedFragment(),
        AnkoLogger {

    private lateinit var checkOutViewModel: CheckOutViewModel

    private var payMethod: PayMethod? = null
    private var price: Double? = null

    private fun enableInput(value: Boolean) {
        pay_btn?.isEnabled = value
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            price = it.getDouble(ARG_PAY_PRICE)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.pay_fragment, container, false)
    }

    // You can safely use ui components here since it is
    // created after onViewCreated.
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // NOTE: ViewModel must be created using the same
        // scope under which the sender is created.

        checkOutViewModel = activity?.run {
            ViewModelProviders.of(this)
                    .get(CheckOutViewModel::class.java)
        } ?: throw Exception("Invalid Activity")


        // Change button text when user selected a pay method.
        checkOutViewModel.methodSelected.observe(this, Observer<PayMethod> {
            payMethod = it

            info("Selected pay method $it")

            val btnText = when (it) {
                PayMethod.ALIPAY -> getString(R.string.pay_method_ali)
                PayMethod.WXPAY -> getString(R.string.pay_method_wechat)
                PayMethod.STRIPE -> getString(R.string.pay_method_stripe)
                else -> {
                    payMethod = null
                    getString(R.string.pay_method_unknown)
                }
            }

            val priceText = getString(R.string.formatter_price, price)

            pay_btn.text = getString(R.string.formatter_check_out, btnText, priceText)
        })

        // In case of payment failure, re-enable the button.
        checkOutViewModel.inputEnabled.observe(this, Observer<Boolean> {
            enableInput(it)
        })


        // Handle pay button
        pay_btn.setOnClickListener {
            if (payMethod == null) {
                toast(R.string.pay_method_unknown)
                return@setOnClickListener
            }

            val pm = payMethod ?: return@setOnClickListener

            // Host activity should show progress bar upon
            // observing payment starts.
            toast(R.string.request_order)

            checkOutViewModel.startPayment(pm)

            enableInput(false)
        }
    }


    companion object {
        private const val ARG_PAY_PRICE = "arg_pay_price"

        @JvmStatic
        fun newInstance(price: Double) = PayFragment().apply {
            arguments = Bundle().apply {
                putDouble(ARG_PAY_PRICE, price)
            }
        }
    }
}
