package com.ft.ftchinese.ui.paywall

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentCustomerServiceBinding
import com.ft.ftchinese.ui.base.IntentsUtil
import com.ft.ftchinese.ui.base.ScopedFragment
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.toast

@Deprecated("Use compose ui")
class CustomerServiceFragment : ScopedFragment() {

    private lateinit var binding: FragmentCustomerServiceBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_customer_service, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.customerServiceEmail.onClick {
            val intent = IntentsUtil.emailCustomerService("FT中文网会员订阅")

            activity?.run {
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                } else {
                    toast(R.string.prompt_no_email_app)
                }
            }
        }
    }

    companion object {

        @JvmStatic
        fun newInstance() =
            CustomerServiceFragment()
    }
}
