package com.ft.ftchinese.user

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ft.ftchinese.R
import com.ft.ftchinese.models.Membership
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.models.Account
import com.ft.ftchinese.models.Subscription
import com.ft.ftchinese.util.RequestCode
import kotlinx.android.synthetic.main.fragment_membership.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast


class MembershipActivity : SingleFragmentActivity(), AnkoLogger {
    override fun createFragment(): Fragment {
        return MembershipFragment.newInstance()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        info("onActivityResult requestCode: $requestCode, resultCode: $resultCode")

        // When user selected wechat pay in PaymentActivity, it kills itself and tells MembershipActivity to finish too.
        // Otherwise after user clicked done button in WXPayEntryActivity, MembershipActivity will be started again, and user see this activity two times after clicked back button.
        if (requestCode == RequestCode.PAYMENT) {
            if (resultCode == Activity.RESULT_OK) {

                val paymentMethod = data?.getIntExtra(EXTRA_PAYMENT_METHOD, 0)
                when (paymentMethod) {
                    Subscription.PAYMENT_METHOD_WX -> {
                        finish()
                    }
                    Subscription.PAYMENT_METHOD_ALI -> {
                        // update ui
                    }
                }
            }
        }
    }

    companion object {
        fun start(context: Context?) {
            val intent = Intent(context, MembershipActivity::class.java)
            context?.startActivity(intent)
        }
    }
}
/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [MembershipFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [MembershipFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class MembershipFragment : Fragment(), AnkoLogger {
    private var mListener: OnFragmentInteractionListener? = null
    private var mUser: Account? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        info("onCreate finished")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_membership, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ctx = try {
            requireContext()
        } catch (e: Exception) {
            return
        }
        mUser = SessionManager.getInstance(ctx).loadUser()

        // User is not logged in
        if (mUser == null) {
            // Do not show membership box
            membership_container.visibility = View.GONE
            // Do not show renewal button
            renewal_button.visibility = View.GONE

            paywall_login_button.setOnClickListener {
                SignInActivity.start(activity)
            }

            // All payment button should jump to login
            subscribe_standard_year.setOnClickListener {
                SignInActivity.start(activity)
            }

            subscribe_standard_month.setOnClickListener {
                SignInActivity.start(activity)
            }

            subscribe_premium_year.setOnClickListener {
                SignInActivity.start(activity)
            }

        } else {
            // Hide login button
            paywall_login_container.visibility = View.GONE

            // Account is logged in
            // Show mUser's membership information
            updateUI()


            // Launch PaymentActivity
            subscribe_standard_year.setOnClickListener {
                PaymentActivity.startForResult(activity, RequestCode.PAYMENT, Membership.TIER_STANDARD, Membership.BILLING_YEARLY)
            }

            subscribe_standard_month.setOnClickListener {
                PaymentActivity.startForResult(activity, RequestCode.PAYMENT, Membership.TIER_STANDARD, Membership.BILLING_MONTHLY)
            }

            subscribe_premium_year.setOnClickListener {
                PaymentActivity.startForResult(activity, RequestCode.PAYMENT, Membership.TIER_PREMIUM, Membership.BILLING_YEARLY)
            }
        }
    }


    private fun updateUI() {

        val user = mUser ?: return
        if (user.isVip) {
            tier_value.text = getString(R.string.member_tier_vip)
            duration_value.text = getString(R.string.vip_duration)

            return
        }

        val cycleText = when(user.membership.billingCycle) {
            Membership.BILLING_YEARLY -> getString(R.string.billing_cycle_year)
            Membership.BILLING_MONTHLY -> getString(R.string.billing_cycle_month)
            else -> ""
        }

        tier_value.text = when (user.membership.tier) {
            Membership.TIER_STANDARD -> getString(R.string.member_tier_standard) + "/" + cycleText
            Membership.TIER_PREMIUM -> getString(R.string.member_tier_premium) + "/" + cycleText
            else -> getString(R.string.member_tier_free)
        }

        // Show expiration date
        duration_value.text = user.membership.expireDate

        // Test if renewal button should be visible.
        // Only the current date to expire date is less than a billing cycle will the user allowed to renew.
        if (user.membership.isRenewable) {
            renewal_button.visibility = View.VISIBLE

            // For renewal use user's current membership tier and billing cycle
            renewal_button.setOnClickListener {
                PaymentActivity.startForResult(activity, RequestCode.PAYMENT, user.membership.tier, user.membership.billingCycle)
            }

        } else {
            renewal_button.visibility = View.GONE
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of MembershipFragment.
         */
        @JvmStatic
        fun newInstance() = MembershipFragment()
    }
}
