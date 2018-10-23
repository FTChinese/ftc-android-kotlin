package com.ft.ftchinese.user

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ft.ftchinese.R
import com.ft.ftchinese.models.ErrorResponse
import com.ft.ftchinese.models.User
import com.ft.ftchinese.models.UserNameUpdate
import com.ft.ftchinese.util.gson
import kotlinx.android.synthetic.main.fragment_username.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

internal class UsernameFragment : Fragment(), AnkoLogger {

    private var mUser: User? = null
    private var job: Job? = null
    private var mListener: OnFragmentInteractionListener? = null

    private var isInProgress: Boolean
        get() = !name_save_button.isEnabled
        set(value) {
            mListener?.onProgress(value)
        }

    private var isInputAllowed: Boolean
        get() = user_name.isEnabled
        set(value) {
            user_name.isEnabled = value
            name_save_button.isEnabled = value
        }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (context is OnFragmentInteractionListener) {
            mListener = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            val userData = it.getString(ARG_USER_DATA)
            mUser = try {
                gson.fromJson<User>(userData, User::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_username, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        current_name.text = if (mUser?.userName.isNullOrBlank()) "未设置" else mUser?.userName

        name_save_button.setOnClickListener {
            attemptSave()
        }
    }

    private fun attemptSave() {
        user_name.error = null
        val userNameStr = user_name.text.toString()

        var cancel = false

        if (userNameStr.isBlank()) {
            user_name.error = getString(R.string.error_field_required)
            cancel = true
        } else if (userNameStr == mUser?.userName) {
            user_name.error = getString(R.string.error_name_unchanged)
            cancel = true
        }

        if (cancel) {
            user_name.requestFocus()
            return
        }

        save(userNameStr)
    }

    private fun save(userName: String) {
        val uuid = mUser?.id ?: return

        isInProgress = true
        isInputAllowed = false

        job = launch(UI) {


            val userNameUpdate = UserNameUpdate(userName)

            try {
                info("Start updating mUser name")

                val userUpdated = userNameUpdate.updateAsync(uuid).await()

                isInProgress = false

                current_name.text = userName

                mListener?.onUserSession(userUpdated)

                toast(R.string.success_saved)
            } catch (e: ErrorResponse) {
                isInProgress = false
                isInputAllowed = true

                when (e.statusCode) {
                    // Should handle duplicate email here.
                    422 -> {
                        toast("用户名无效或已经被占用")
                    }
                    404 -> {
                        toast("用户不存在")
                    }
                    400 -> {
                        toast("提交了非法的JSON")
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

    companion object {
        private const val ARG_USER_DATA = "user_data"
        fun newInstance(user: User?) = UsernameFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_USER_DATA, gson.toJson(user))
            }
        }
    }
}