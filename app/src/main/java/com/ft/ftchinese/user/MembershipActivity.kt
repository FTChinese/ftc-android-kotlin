package com.ft.ftchinese.user

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.alipay.sdk.app.PayTask
import com.ft.ftchinese.BuildConfig

import com.ft.ftchinese.R
import com.ft.ftchinese.models.Membership
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.models.User
import com.ft.ftchinese.util.RequestCode
import com.tencent.mm.opensdk.constants.Build
import com.tencent.mm.opensdk.modelpay.PayReq
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.android.synthetic.main.fragment_membership.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

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
    private var mDialog: PaymentFragment? = null
    private var user: User? = null
    private var wxApi: IWXAPI? = null

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

        wxApi = WXAPIFactory.createWXAPI(context, BuildConfig.WECAHT_APP_ID)

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
        user = SessionManager.getInstance(ctx).loadUser()

        // User is not logged in
        if (user == null) {
            membership_container.visibility = View.GONE

            paywall_login_button.setOnClickListener {
                SignInOrUpActivity.startForResult(activity, RequestCode.SIGN_IN)
            }

            subscribe_standard_year.setOnClickListener {
                SignInOrUpActivity.startForResult(activity, RequestCode.SIGN_IN)
            }

            subscription_premium_btn.setOnClickListener {
                SignInOrUpActivity.startForResult(activity, RequestCode.SIGN_IN)
            }
        } else {
            // User is logged in
            // Show user's membership information
            member_value.text = when (user?.membership?.tier) {
                Membership.TIER_FREE -> getString(R.string.member_type_free)
                Membership.TIER_STANDARD -> getString(R.string.member_type_standard)
                Membership.TIER_PREMIUM -> getString(R.string.member_type_premium)
                else -> null
            }

            // Show expiration date
            duration_value.text = user?.membership?.localizedExpireDate

            // Hide login button
            paywall_login_container.visibility = View.GONE

            // Show a dialog so that user could select payment channel
            subscribe_standard_year.setOnClickListener {
                val dialogFrag = PaymentFragment.newInstance(Membership.TIER_STANDARD, Membership.BILLING_YEARLY)
                dialogFrag.setTargetFragment(this, REQUEST_PAY)
                dialogFrag.show(fragmentManager, "DialogPayment")
                mDialog = dialogFrag
            }

            subscription_premium_btn.setOnClickListener {
                val dialogFrag = PaymentFragment.newInstance(Membership.TIER_PREMIUM, Membership.BILLING_YEARLY)
                dialogFrag.setTargetFragment(this, REQUEST_PAY)
                dialogFrag.show(fragmentManager, "DialogPayment")
                mDialog = dialogFrag
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            return
        }

        if (requestCode == REQUEST_PAY) {
            val memberTier = data?.getStringExtra(PaymentFragment.ARG_MEMBERSHIP_TIER)
            val billingCycle = data?.getStringExtra(PaymentFragment.ARG_BILLING_CYCLE)
            val paymentMethod = data?.getIntExtra(PaymentFragment.EXTRA_PAYMENT_METHOD, 0)

            if (memberTier == null || billingCycle == null || paymentMethod == null) {
                return
            }

            when (paymentMethod) {
                Membership.PAYMENT_METHOD_WX -> {
                    val supportedApi = wxApi?.wxAppSupportAPI
                    if (supportedApi != null) {
                        val isPaySupported = supportedApi >= Build.PAY_SUPPORTED_SDK_INT
                        toast("WX pay supported: $isPaySupported")
                    }

                    launch(UI) {
                        val prepayOrder = try {
                            user?.wxPrepayOrderAsync(memberTier, billingCycle)?.await()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            toast("Network error")
                            return@launch
                        }

                        if (prepayOrder == null) {
                            toast("Cannot create order for wechat")
                            return@launch
                        }

                        info("Prepay order: $prepayOrder")

                        val req = PayReq()
                        req.appId = prepayOrder.appid
                        req.partnerId = prepayOrder.partnerid
                        req.prepayId = prepayOrder.prepayid
                        req.nonceStr = prepayOrder.noncestr
                        req.timeStamp = prepayOrder.timestamp
                        req.packageValue = prepayOrder.`package`
                        req.sign = prepayOrder.sign

                        bg {
                            wxApi?.sendReq(req)
                        }
                    }
                }

                Membership.PAYMENT_METHOD_ALI -> {
                    launch(UI) {
                        mListener?.onProgress(true)
                        toast("创建订单...")
                        val aliOrder = try {
                            user?.alipayOrderAsync(memberTier, billingCycle)?.await()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            info("$e")
                            return@launch
                        } finally {
                            mListener?.onProgress(false)
                        }

                        info("Alipay order: $aliOrder")

                        val payJob = bg {
                            val alipay = PayTask(activity)
                            val result = alipay.payV2(aliOrder?.order, true)

                            result
                        }

                        try {
                            val result = payJob.await()

                            info("Alipay result: $result")
                        } catch (e: Exception) {
                            e.printStackTrace()
                            toast("$e")
                        }
                    }
                }

                Membership.PAYMENT_METHOD_STRIPE -> {
                    toast("Stripe payment is not implemented yet")
                }
                else -> {
                    toast("Unknown payment method")
                }
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
        mDialog = null
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
