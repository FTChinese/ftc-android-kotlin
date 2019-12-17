package com.ft.ftchinese.viewmodel

import android.util.Patterns
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.Result
import com.ft.ftchinese.model.reader.Credentials
import com.ft.ftchinese.model.reader.WxSession
import com.ft.ftchinese.repository.ReaderRepo
import com.ft.ftchinese.ui.login.AccountResult
import com.ft.ftchinese.ui.login.FindEmailResult
import com.ft.ftchinese.ui.login.LoginFormState
import com.ft.ftchinese.util.ClientError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class LoginViewModel : ViewModel(), AnkoLogger {

    val inProgress: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    val loginFormState: MutableLiveData<LoginFormState> by lazy {
        MutableLiveData<LoginFormState>()
    }

    val emailResult: MutableLiveData<FindEmailResult> by lazy {
        MutableLiveData<FindEmailResult>()
    }

    val accountResult: MutableLiveData<AccountResult> by lazy {
        MutableLiveData<AccountResult>()
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
            loginFormState.value = LoginFormState(error = R.string.error_invalid_password)
        } else{
            loginFormState.value = LoginFormState(isPasswordValid = true)
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

                emailResult.value = FindEmailResult(
                        success = Pair(email, ok)
                )

            } catch (e: ClientError) {

                if (e.statusCode== 404) {
                    emailResult.value = FindEmailResult(
                            success = Pair(email, false)
                    )
                    return@launch
                }

                emailResult.value = FindEmailResult(
                        exception = e
                )

            } catch (e:Exception) {

                emailResult.value = FindEmailResult(
                        exception = e
                )
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

                    accountResult.value = AccountResult(
                            error = R.string.loading_failed
                    )

                    return@launch
                }

                accountResult.value = AccountResult(
                        success = account
                )
            } catch (e: ClientError) {
                val msgId = if (e.statusCode == 404) {
                    R.string.error_invalid_password
                } else {
                    e.parseStatusCode()
                }

                accountResult.value = AccountResult(
                        error = msgId,
                        exception = e
                )

            } catch (e: Exception) {

                accountResult.value = AccountResult(
                        exception = e
                )
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
                    accountResult.value = AccountResult(
                            error = R.string.loading_failed
                    )
                    return@launch
                }

                accountResult.value = AccountResult(
                        success = account
                )

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
                    e.parseStatusCode()
                }

                accountResult.value = AccountResult(
                        error = msgId,
                        exception = e
                )
            } catch (e: Exception) {
                accountResult.value = AccountResult(exception = e)
            }
        }
    }

    fun requestResettingPassword(email: String) {
        viewModelScope.launch {
            try {
                val ok = withContext(Dispatchers.IO) {
                    ReaderRepo.passwordResetLetter(email)
                }

                pwResetLetterResult.value = Result.Success(ok)

            } catch (e: ClientError) {
                if (e.statusCode == 404) {
                    pwResetLetterResult.value = Result.LocalizedError(R.string.api_email_not_found)
                } else {
                    pwResetLetterResult.value = Result.Error(e)
                }
            } catch (e: Exception) {
                pwResetLetterResult.value = Result.Error(e)
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
                info("Starte requesting wechat oauth session data")
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

                // via wechat only.
                info("Start loading wechat account")

                // Here won't throw an errors.
//                loadWxAccount(sess)
            } catch (e: Exception) {
                // If the error is ClientError,
                // Possible 422 error key: code_missing_field, code_invalid.
                // We cannot make sure the exact meaning of each error, just
                // show user API's error message.

                info("Exception: $e")

                wxSessionResult.value = Result.Error(e)
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

                info("Loaded wechat account: $account")

                accountResult.value = AccountResult(success = account)
            } catch (e: ClientError) {
                info("Retrieving wechat account error $e")
                val msgId = if (e.statusCode == 404) {
                    R.string.loading_failed
                } else {
                    e.parseStatusCode()
                }

                accountResult.value = AccountResult(
                        error = msgId,
                        exception = e
                )

            } catch (e: Exception) {

                info(e)
                accountResult.value = AccountResult(exception = e)
            }
        }
    }

    /**
     * Load account after user's password verified
     * or signed up.
     */
//    private suspend fun loadFtcAccount(userId: String) {
//        try {
//            val account = withContext(Dispatchers.IO) {
//                FtcUser(id = userId).fetchAccount()
//            }
//
//            accountResult.value = AccountResult(success = account)
//
//        } catch (e: ClientError) {
//            val msgId = if (e.statusCode == 404) {
//                R.string.loading_failed
//            } else {
//                e.parseStatusCode()
//            }
//
//            accountResult.value = AccountResult(
//                    error = msgId,
//                    exception = e
//            )
//
//        } catch (e: Exception) {
//
//            accountResult.value = AccountResult(exception = e)
//        }
//    }


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
