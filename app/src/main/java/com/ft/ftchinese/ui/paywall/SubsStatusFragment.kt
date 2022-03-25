package com.ft.ftchinese.ui.paywall

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentSubsStatusBinding
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.ui.login.AuthActivity
import com.ft.ftchinese.ui.product.ProductViewModel


/**
 * A simple [Fragment] subclass.
 * Use the [SubsStatusFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@Deprecated("Use comppose ui")
class SubsStatusFragment : Fragment() {

    private lateinit var productViewModel: ProductViewModel
    private lateinit var binding: FragmentSubsStatusBinding


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_subs_status, container, false)

        // Inflate the layout for this fragment
        return binding.root
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        productViewModel = activity?.run {
            ViewModelProvider(this).get(ProductViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        productViewModel.accountChanged.observe(viewLifecycleOwner, Observer {

            if (it == null) {
                binding.loggedIn = false

                binding.loginButton.setOnClickListener {
                    AuthActivity.startForResult(activity)
                }

                return@Observer
            }

            binding.loggedIn = true

            binding.expiredWarning = buildExpiredWarning(it.membership)
        })
    }

    // Create an expiration reminder if membership exists but expired; otherwise returns null.
    private fun buildExpiredWarning(m: Membership?): String? {
        if (m == null) {
            return null
        }

        if (m.tier == null) {
            return null
        }

        if (!m.autoRenewOffExpired) {
            return null
        }

        val tierText = when (m.tier) {
            Tier.STANDARD -> getString(R.string.tier_standard)
            Tier.PREMIUM -> getString(R.string.tier_premium)
        }

        return getString(R.string.member_expired_on, tierText, m.expireDate)
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment SubsStatusFragment.
         */
        @JvmStatic
        fun newInstance() = SubsStatusFragment()
    }
}
