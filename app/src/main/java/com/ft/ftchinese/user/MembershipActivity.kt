package com.ft.ftchinese.user

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
import com.ft.ftchinese.util.RequestCode
import kotlinx.android.synthetic.main.fragment_membership.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class MembershipActivity : SingleFragmentActivity() {
    override fun createFragment(): Fragment {
        return MembershipFragment.newInstance()
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
        val user = SessionManager.getInstance(ctx).loadUser()

        // User is not logged in
        if (user == null) {
            membership_container.visibility = View.GONE

            paywall_login_button.setOnClickListener {
                SignInOrUpActivity.startForResult(activity, RequestCode.SIGN_IN)
            }

            subscription_standard_btn.setOnClickListener {
                SignInOrUpActivity.startForResult(activity, RequestCode.SIGN_IN)
            }

            subscription_premium_btn.setOnClickListener {
                SignInOrUpActivity.startForResult(activity, RequestCode.SIGN_IN)
            }
        } else {
            // User is logged in
            // Show user's membership information
            member_value.text = when (user.membership.type) {
                Membership.TYPE_FREE -> getString(R.string.member_type_free)
                Membership.TYPE_STANDARD -> getString(R.string.member_type_standard)
                Membership.TYPE_PREMIUM -> getString(R.string.member_type_premium)
                else -> null
            }

            duration_value.text = user.membership.localizedExpireDate

            // Hide login button
            paywall_login_container.visibility = View.GONE

            // Show a dialog so that user could select payment channel
            subscription_standard_btn.setOnClickListener {
                val dialogFrag = PaymentFragment.newInstance(Membership.TYPE_STANDARD)
                dialogFrag.setTargetFragment(this, REQUEST_PAY)
                dialogFrag.show(fragmentManager, "DialogPayment")
            }

            subscription_premium_btn.setOnClickListener {
                val dialogFrag = PaymentFragment.newInstance(Membership.TYPE_PREMIUM)
                dialogFrag.setTargetFragment(this, REQUEST_PAY)
                dialogFrag.show(fragmentManager, "DialogPayment")
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    companion object {
        const val REQUEST_PAY = 1
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
