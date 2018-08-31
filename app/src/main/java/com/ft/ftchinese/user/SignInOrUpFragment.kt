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
import com.ft.ftchinese.MainActivity
import com.ft.ftchinese.R
import com.ft.ftchinese.models.Account
import com.ft.ftchinese.models.ErrorResponse
import com.ft.ftchinese.models.User
import com.google.gson.JsonSyntaxException
import kotlinx.android.synthetic.main.fragment_sign_in_or_up.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast
import java.io.IOException

internal class SignInOrUpFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor>, AnkoLogger {

    private var job: Job? = null
    private var listener: OnFragmentInteractionListener? = null
    private var isInProgress: Boolean
        get() = !email_sign_in_button.isEnabled
        set(value) {
            listener?.onProgress(value)
            email_sign_in_button.isEnabled = !value
        }

    private var isInputAllowed: Boolean
        get() = email.isEnabled
        set(value) {
            email.isEnabled = value
            password.isEnabled = value
            email_sign_in_button.isEnabled = value
            email_sign_up_button.isEnabled = value
        }

    private var usedFor: Int? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (context is OnFragmentInteractionListener) {
            listener = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        usedFor = arguments?.getInt(ARG_FRAGMENT_USAGE)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_sign_in_or_up, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        when (usedFor) {
            USED_FOR_LOGIN -> {
                email_sign_up_button.visibility = View.GONE
                sign_in.visibility = View.GONE
            }
            USED_FOR_SIGN_UP -> {
                email_sign_in_button.visibility = View.GONE
                wechat_sign_in_button.visibility = View.GONE
                reset_password.visibility = View.GONE
                sign_up.visibility = View.GONE
            }
        }

        password.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                validate()

                return@setOnEditorActionListener true
            }

            false
        }

        email_sign_up_button.setOnClickListener {
            validate()
        }

        email_sign_in_button.setOnClickListener {
            validate()
        }

        reset_password.setOnClickListener {
            ForgotPasswordActivity.start(context)
        }

        sign_up.setOnClickListener {
            Registration.startForResult(activity, MainActivity.REQUEST_CODE_SIGN_UP)
        }

        sign_in.setOnClickListener {
            LoginActivity.startForResult(activity, MainActivity.REQUEST_CODE_SIGN_IN)
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
    private fun validate() {
        email.error = null
        password.error = null

        val emailStr = email.text.toString()
        val passwordStr = password.text.toString()

        var cancel = false
        var focusView: View? = null

        if (!TextUtils.isEmpty(passwordStr) && !isPasswordValid(passwordStr)) {
            password.error = getString(R.string.error_invalid_password)
            focusView = password
            cancel = true
        }

        if (TextUtils.isEmpty(emailStr)) {
            email.error = getString(R.string.error_field_required)
            focusView = email
            cancel = true
        } else if (!isEmailValid(emailStr)) {
            email.error = getString(R.string.error_invalid_email)
            focusView = email
            cancel = true
        }

        if (cancel) {
            focusView?.requestFocus()

            return
        }

        authenticate(emailStr, passwordStr)
    }

    private fun authenticate(email: String, password: String) {
        isInProgress = true
        isInputAllowed = false

        job = launch(UI) {
            val account = Account(email = email, password = password)
            try {
                var user: User? = null

                when (usedFor) {
                    USED_FOR_LOGIN -> {
                        info("Start loggin in")

                        user = account.login()
                    }
                    USED_FOR_SIGN_UP -> {
                        info("Start signing up")

                        user = account.create()
                    }
                }

                isInProgress = false

                if (user == null) {
                    info("Failed to get user data. Stop")
                    return@launch
                }

                info("User $user")

                user.save(context)

                activity?.setResult(Activity.RESULT_OK)
                activity?.finish()

            } catch (e: ErrorResponse) {
                info(e.message)
                isInProgress = false
                isInputAllowed = true

                when (e.statusCode) {
                    404 -> {
                        toast("用户名或密码错误")
                    }
                    400 -> {
                        toast("提交了非法的JSON")
                    }
                    422 -> {
                        toast("用户名或密码非法")
                    }
                    429 -> {
                        toast("创建账号过于频繁，请稍后再试")
                    }
                }
            } catch (e: Exception) {
                isInProgress = false
                isInputAllowed = true

                handleException(e)
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
        val IS_PRIMARY = 1
    }

    companion object {
        private const val REQUEST_READ_CONTACTS = 0
        private const val ARG_FRAGMENT_USAGE = "arg_fragment_usage"
        const val USED_FOR_LOGIN = 1
        const val USED_FOR_SIGN_UP = 2

        fun newInstance(usedFor: Int): SignInOrUpFragment {
            val fragment = SignInOrUpFragment()
            fragment.arguments = Bundle().apply {
                putInt(ARG_FRAGMENT_USAGE, usedFor)
            }

            return fragment
        }
    }
}