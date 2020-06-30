package com.ft.ftchinese.viewmodel

import android.util.Patterns
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Credentials
import com.ft.ftchinese.model.reader.PwResetVerifier
import com.ft.ftchinese.model.reader.WxSession
import com.ft.ftchinese.repository.ReaderRepo
import com.ft.ftchinese.repository.ClientError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class LoginViewModel : ViewModel(), AnkoLogger {

    val inProgress: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
    val isNetworkAvailable = MutableLiveData<Boolean>()

    val loginFormState: MutableLiveData<LoginFormState> by lazy {
        MutableLiveData<LoginFormState>()
    }

    val emailResult: MutableLiveData<Result<Existence>> by lazy {
        MutableLiveData<Result<Existence>>()
    }

    val accountResult: MutableLiveData<Result<Account>> by lazy {
        MutableLiveData<Result<Account>>()
    }

    val wxSessionResult: MutableLiveData<Result<WxSession>> by lazy {
        MutableLiveData<Result<WxSession>>()
    }

    val pwResetLetterResult: MutableLiveData<Result<Boolean>> by lazy {
        MutableLiveData<Result<Boolean>>()
    }

    fun emailDataChanged(email: String) {
        if (!isEmailValid(email)) {
            loginFormState.value = LoginFormState(error = R.string.error_invalid_email)
        } else {
            loginFormState.value = LoginFormState(isEmailValid = true)
        }
    }

    fun passwordDataChanged(password: String) {
        if (!isPasswordValid(password)) {
            loginFormState.value = LoginFormState(
                error = R.string.error_invalid_password
            )
        } else{
            loginFormState.value = LoginFormState(
                isPasswordValid = true
            )
        }
    }

    // See https://developer.android.com/kotlin/ktx
    // https://developer.android.com/topic/libraries/architecture/coroutines
    fun checkEmail(email: String) {

        viewModelScope.launch {

            try {
                val ok = withContext(Dispatchers.IO) {

                    ReaderRepo.emailExists(email)
                }


                emailResult.value = Result.Success(Existence(
                        value = email,
                        found = ok
                ))
            } catch (e: ClientError) {

                if (e.statusCode== 404) {
                    emailResult.value = Result.Success(Existence(
                            value = email,
                            found = false
                    ))
                    return@launch
                }

                emailResult.value = parseApiError(e)

            } catch (e:Exception) {

                emailResult.value = parseException(e)
            }
        }
    }

    fun login(c: Credentials) {

        viewModelScope.launch {
            try {
                val account = withContext(Dispatchers.IO) {
                    ReaderRepo.login(c)
                }

                if (account == null) {

                    accountResult.value = Result.LocalizedError(R.string.loading_failed)
                    return@launch
                }

                accountResult.value = Result.Success(account)
            } catch (e: ClientError) {

                accountResult.value = if (e.statusCode == 404) {
                    Result.LocalizedError(R.string.error_invalid_password)
                } else {
                    parseApiError(e)
                }

            } catch (e: Exception) {
                accountResult.value = parseException(e)
            }
        }
    }

    /**
     * Handles both a new user signup, or wechat-logged-in
     * user trying to link to a new account.
     */
    fun signUp(c: Credentials) {

        viewModelScope.launch {
            try {
                val account = withContext(Dispatchers.IO) {
                    ReaderRepo.signUp(c)
                }

                if (account == null) {

                    accountResult.value = Result.LocalizedError(R.string.loading_failed)
                    return@launch
                }

                accountResult.value = Result.Success(account)
            } catch (e: ClientError) {
                val msgId = if (e.statusCode == 422) {
                    when (e.error?.key) {
                        "email_already_exists" -> R.string.api_email_taken
                        "email_invalid" -> R.string.error_invalid_email
                        "password_invalid" -> R.string.error_invalid_password
                        // handles wechat user sign up.
                        "account_link_already_taken" -> R.string.api_wechat_already_linked
                        "membership_link_already_taken" -> R.string.api_wechat_member_already_linked
                        else -> null
                    }
                } else {
                    null
                }

                accountResult.value = if (msgId != null) {
                    Result.LocalizedError(msgId)
                } else {
                    parseApiError(e)
                }

            } catch (e: Exception) {
                accountResult.value = parseException(e)
            }
        }
    }

    /**
     * Uses wechat authrozation code to get an access token, and then use the
     * token to get user info.
     * API responds with WxSession data to uniquely identify this login
     * session.
     * You can use the session data later to retrieve user account.
     */
    fun wxLogin(code: String) {
        viewModelScope.launch {
            try {
                info("Start requesting wechat oauth session data")
                val sess = withContext(Dispatchers.IO) {
                    ReaderRepo.wxLogin(code)
                }

                // Fetched wx session data and send it to
                // UI thread for saving, and then continues
                // to fetch account data.
                if (sess == null) {
                    info("Wechat oauth session is null")
                    wxSessionResult.value = Result.LocalizedError(R.string.loading_failed)
                    return@launch
                }

                wxSessionResult.value = Result.Success(sess)

            } catch (e: Exception) {
                // If the error is ClientError,
                // Possible 422 error key: code_missing_field, code_invalid.
                // We cannot make sure the exact meaning of each error, just
                // show user API's error message.

                info(e)

                wxSessionResult.value = parseException(e)
            }
        }
    }

    /**
     * Load account after user performed wechat authorization
     */
    fun loadWxAccount(wxSession: WxSession) {
        info("Start retrieving wechat account")

        viewModelScope.launch {
            try {
                val account = withContext(Dispatchers.IO) {
                    ReaderRepo.loadWxAccount(wxSession.unionId)
                }

                if (account == null) {
                    accountResult.value = Result.LocalizedError(R.string.loading_failed)
                    return@launch
                }

                info("Loaded wechat account: $account")

                accountResult.value = Result.Success(account)
            } catch (e: ClientError) {
                info("Retrieving wechat account error $e")

                accountResult.value = if (e.statusCode == 404) {
                    Result.LocalizedError(R.string.loading_failed)
                } else {
                    parseApiError(e)
                }

            } catch (e: Exception) {

                info(e)
                accountResult.value = parseException(e)
            }
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return if (!email.contains('@')) {
            false
        } else {
            Patterns.EMAIL_ADDRESS.matcher(email).matches()
        }
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length >= 6
    }
}
