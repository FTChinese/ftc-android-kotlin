package com.ft.ftchinese.user

import android.Manifest.permission.READ_CONTACTS
import android.app.Activity
import android.app.LoaderManager.LoaderCallbacks
import android.content.*
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
import android.widget.Toast
import com.ft.ftchinese.R
import com.ft.ftchinese.models.Account
import com.ft.ftchinese.models.ErrorResponse
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.support.v4.toast

class SignInActivity : SingleFragmentActivity() {

    override fun createFragment(): Fragment {
        return SignInFragment.newInstance()
    }

    companion object {
        /**
         * When launching SignInActivity, you usually should notify the caller activity login result.
         * @param activity The activity that launched this activity.
         * @param requestCode returned to the calling activity's onActivityResult()
         */
        fun startForResult(activity: Activity, requestCode: Int) {
            val intent = Intent(activity, SignInActivity::class.java)

            activity.startActivityForResult(intent, requestCode)
        }
    }
}
class SignInFragment : Fragment(), LoaderCallbacks<Cursor>, AnkoLogger {

    private var job: Job? = null
    private var listener: OnFragmentInteractionListener? = null


    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (context is OnFragmentInteractionListener) {
            listener = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        password.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                attemptLogin()

                return@setOnEditorActionListener true
            }

            false
        }

        email_sign_in_button.setOnClickListener {
            attemptLogin()
        }

        reset_password.setOnClickListener {
            ForgotPasswordActivity.start(context)
        }

        sign_up.setOnClickListener {
            SignupActivity.start(context)
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

        if (context?.checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true
        }

        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(email, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok) {
                        requestPermissions(arrayOf(READ_CONTACTS), REQUEST_READ_CONTACTS)
                    }
        } else {
            requestPermissions(arrayOf(READ_CONTACTS), REQUEST_READ_CONTACTS)
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
    private fun attemptLogin() {
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
        } else {
            listener?.onProgress(true)
            email_sign_in_button.isEnabled = false

            authenticate(emailStr, passwordStr)
        }
    }

    private fun authenticate(email: String, password: String) {
        job = launch(UI) {
            val account = Account(email = email, password = password)
            try {
                val user = account.login()
                listener?.onProgress(false)

                if (user == null) {
                    toast("登录失败！请重试")
                    email_sign_in_button.isEnabled = true
                    return@launch
                }
                user.save(context)
                activity?.setResult(Activity.RESULT_OK)
                activity?.finish()

            } catch (e: ErrorResponse) {
                listener?.onProgress(false)
                email_sign_in_button.isEnabled = true

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
                }


            } catch (e: Exception) {
                listener?.onProgress(false)
                email_sign_in_button.isEnabled = true

                toast("Error!")
            }
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return email.contains("@")
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length > 4
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

        fun newInstance(): SignInFragment {
            return SignInFragment()
        }
    }
}