package com.ft.ftchinese.user

import android.Manifest
import android.app.Activity
import android.app.LoaderManager
import android.content.Context
import android.content.CursorLoader
import android.content.Loader
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.models.*
import com.ft.ftchinese.util.*
import com.google.firebase.analytics.FirebaseAnalytics
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.android.synthetic.main.fragment_sign_in_or_up.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

class SignInOrUpFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor>, AnkoLogger {

    private var job: Job? = null
    private var listener: OnFragmentInteractionListener? = null

    private var mFirebaseAnalytics: FirebaseAnalytics? = null
    private var mSession: SessionManager? = null
    private var mWxManager: WxManager? = null

    private var wxApi: IWXAPI? = null

    // Show or hide progress bar.
    private var isInProgress: Boolean
        get() = !email_sign_in_button.isEnabled
        set(value) {
            listener?.onProgress(value)
            email_sign_in_button?.isEnabled = !value
        }

    // Enable or disabled input.
    // When progress bar is visible, disable any input and button;
    // When progress bar is gone and login/signup failed, reenable input;
    // When progress bar is gone and login/signpu succeeded, destroy the activity and it's not necessary to enable input.
    private var isInputAllowed: Boolean
        get() = email.isEnabled
        set(value) {
            email?.isEnabled = value
            password?.isEnabled = value
            email_sign_in_button?.isEnabled = value
            email_sign_up_button?.isEnabled = value
        }

    private var usedFor: Int? = null

    private fun showProgress(value: Boolean) {
        listener?.onProgress(value)
        email_sign_in_button?.isEnabled = !value
    }

    private fun allowInput(value: Boolean) {
        email?.isEnabled = value
        password?.isEnabled = value
        email_sign_in_button?.isEnabled = value
        email_sign_up_button?.isEnabled = value
    }

    // Cast parent activity to OnFragmentInteractionListener
    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (context is OnFragmentInteractionListener) {
            listener = context
        }

        if (context != null) {
            mFirebaseAnalytics = FirebaseAnalytics.getInstance(context)
            mSession = SessionManager.getInstance(context)
            mWxManager = WxManager.getInstance(context)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Init Wechat API
        wxApi = WXAPIFactory.createWXAPI(context, BuildConfig.WX_SUBS_APPID)
        wxApi?.registerApp(BuildConfig.WX_SUBS_APPID)

        // Decide whether this is used for login or signup.
        usedFor = arguments?.getInt(ARG_FRAGMENT_USAGE)
    }

    // Inflate UI from fragment_sign_in_or_up file.
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_sign_in_or_up, container, false)
    }

    // After UI is created.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Display different part of UI according to how it is used.
        when (usedFor) {
            // For login, hide signup button.
            USED_FOR_SIGN_IN -> {
                email_sign_up_button.visibility = View.GONE
                go_to_sign_in.visibility = View.GONE
            }
            // For signup, hide login button, and reset password and sign up button.
            USED_FOR_SIGN_UP -> {
                email_sign_in_button.visibility = View.GONE
                go_to_reset_password.visibility = View.GONE
                go_to_sign_up.visibility = View.GONE
            }
        }

        password.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                validate()

                return@setOnEditorActionListener true
            }

            false
        }

        // When user pressed signup button, validate data.
        email_sign_up_button.setOnClickListener {
            validate()
        }

        // When user pressed login button, validate data
        email_sign_in_button.setOnClickListener {
            validate()
        }

        // When user clicked reset password, launch ForgotPasswordActivity.
        go_to_reset_password.setOnClickListener {
            ForgotPasswordActivity.start(context)
        }

        // When user clicked sign up, launch sign up activity.
        go_to_sign_up.setOnClickListener {
            SignUpActivity.start(activity)
        }

        go_to_sign_in.setOnClickListener {
            SignInActivity.startForResult(activity)
        }

        // Respond to wechat login button
        wechat_sign_in_button.setOnClickListener {

            val nonce = generateNonce(5)
            info("Wechat OAuth state: $nonce")

            mWxManager?.saveState(nonce)

            val req = SendAuth.Req()
            req.scope = "snsapi_userinfo"
            req.state = nonce
            wxApi?.sendReq(req)

            activity?.finish()
        }
    }

    private fun populateAutoComplete() {
        if (!mayRequestContacts()) {
            return
        }

        activity?.loaderManager?.initLoader(0, null, this)
    }

    private fun mayRequestContacts(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }

        if (context?.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true
        }

        if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
            Snackbar.make(email, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok) {
                        requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), REQUEST_READ_CONTACTS)
                    }
        } else {
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), REQUEST_READ_CONTACTS)
        }

        return false
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete()
            }
        }
    }

    // Validate user input.
    private fun validate() {
        email.error = null
        password.error = null

        val emailStr = email.text.toString()
        val passwordStr = password.text.toString()

        var cancel = false
        var focusView: View? = null

        if (TextUtils.isEmpty(emailStr)) {
            email.error = getString(R.string.error_field_required)
            focusView = email
            cancel = true
        } else if (!isEmailValid(emailStr)) {
            email.error = getString(R.string.error_invalid_email)
            focusView = email
            cancel = true
        }

        if (TextUtils.isEmpty(passwordStr)) {
            password.error = getString(R.string.error_field_required)
            focusView = password
            cancel = true
        }

        when (usedFor) {
            // For login limit password length to 6
            USED_FOR_SIGN_IN -> {
                if (passwordStr.length < 6) {
                    password.error = getString(R.string.error_invalid_password)
                    focusView = password
                    cancel = true
                }
            }

            // For new signup limit password length to 8
            USED_FOR_SIGN_UP -> {
                if (!isPasswordValid(passwordStr)) {
                    password.error = getString(R.string.error_invalid_password)
                    focusView = password
                    cancel = true
                }
            }
        }


        if (cancel) {
            focusView?.requestFocus()

            return
        }

        authenticate(emailStr, passwordStr)
    }

    // Send user credentials to API.
    private fun authenticate(email: String, password: String) {
        if (activity?.isNetworkConnected() != true) {
            toast(R.string.prompt_no_network)

            return
        }

        showProgress(true)
        allowInput(false)

        job = GlobalScope.launch(Dispatchers.Main) {
            try {

                var account: Account? = null

                val bundle = Bundle().apply {
                    putString(FirebaseAnalytics.Param.METHOD, "email")
                }

                when (usedFor) {
                    USED_FOR_SIGN_IN -> {
                        mFirebaseAnalytics?.logEvent(FirebaseAnalytics.Event.LOGIN, bundle)
                        info("Start log in")
                        account = withContext(Dispatchers.IO) {
                            Login(email, password).send()
                        }
                    }
                    USED_FOR_SIGN_UP -> {
                        mFirebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SIGN_UP, bundle)
                        info("Start signing up")
                        account = withContext(Dispatchers.IO) {
                            SignUp(email, password).send()
                        }
                    }
                }

                showProgress(false)

                if (account == null) {
                    isInputAllowed = true

                    when (usedFor) {
                        USED_FOR_SIGN_IN -> toast(R.string.error_login)
                        USED_FOR_SIGN_UP -> toast(R.string.error_sign_up)
                    }


                    return@launch
                }

                mSession?.saveAccount(account)
                activity?.setResult(Activity.RESULT_OK)
                activity?.finish()

            } catch (e: ClientError) {
                showProgress(false)
                allowInput(true)

                info("API error response: $e")

                handleClientError(e)

            } catch (e: Exception) {
                showProgress(false)
                allowInput(true)

                activity?.handleException(e)
            }
        }
    }

    private fun handleClientError(resp: ClientError) {

        // Hide progress bar, enable input.
        when (resp.statusCode) {
            // User is not found, or password is wrong
            404, 403 -> {
                toast(R.string.api_wrong_credentials)
            }
            else -> {
                activity?.handleApiError(resp)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        return CursorLoader(
                context,
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI, ContactsContract.Contacts.Data.CONTENT_DIRECTORY),
                ProfileQuery.PROJECTION,
                ContactsContract.Contacts.Data.MIMETYPE + " = ?",
                arrayOf(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE),
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC"
        )
    }

    override fun onLoadFinished(cursorLoader: Loader<Cursor>, cursor: Cursor) {
        val emails = ArrayList<String>()
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS))
            cursor.moveToNext()
        }

        addEmailsToAutoComplete(emails)
    }

    override fun onLoaderReset(loader: Loader<Cursor>?) {

    }

    private fun addEmailsToAutoComplete(emailAddressCollection: List<String>) {
        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, emailAddressCollection)

        email.setAdapter(adapter)
    }

    object ProfileQuery {
        val PROJECTION = arrayOf(
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY)
        val ADDRESS = 0
    }

    companion object {
        private const val REQUEST_READ_CONTACTS = 0
        private const val ARG_FRAGMENT_USAGE = "arg_fragment_usage"
        private const val USED_FOR_SIGN_IN = 1
        private const val USED_FOR_SIGN_UP = 2

        fun newInstanceSignIn() = SignInOrUpFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_FRAGMENT_USAGE, USED_FOR_SIGN_IN)
            }
        }

        fun newInstanceSignUp() = SignInOrUpFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_FRAGMENT_USAGE, USED_FOR_SIGN_UP)
            }
        }
    }
}