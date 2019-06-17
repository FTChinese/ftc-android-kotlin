package com.ft.ftchinese.ui.login

import android.net.Uri
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.Credentials
import com.ft.ftchinese.model.FtcUser
import com.ft.ftchinese.model.WxOAuth
import com.ft.ftchinese.model.WxSession
import com.ft.ftchinese.util.ClientError
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.NextApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginViewModel : ViewModel() {

    val inProgress = MutableLiveData<Boolean>()
    val inputEnabled = MutableLiveData<Boolean>()

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> =_loginForm

    private val _emailResult = MutableLiveData<FindEmailResult>()
    val emailResult: LiveData<FindEmailResult> =_emailResult

    private val _loginResult = MutableLiveData<AccountResult>()
    val loginResult: LiveData<AccountResult> = _loginResult

    private val _wxOAuthResult = MutableLiveData<WxOAuthResult>()
    val wxOAuthResult: LiveData<WxOAuthResult> = _wxOAuthResult

    fun emailDataChanged(email: String) {
        if (!isEmailValid(email)) {
            _loginForm.value = LoginFormState(emailError = R.string.error_invalid_email)
        } else {
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }

    fun passwordDataChanged(password: String) {
        if (!isPasswordValid(password)) {
            _loginForm.value = LoginFormState(passwordError = R.string.error_invalid_password)
        } else{
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }

    // See https://developer.android.com/kotlin/ktx
    // https://developer.android.com/topic/libraries/architecture/coroutines
    fun checkEmail(email: String) {

        viewModelScope.launch {

            val apiUrl = Uri.parse(NextApi.EMAIL_EXISTS)
                    .buildUpon()
                    .appendQueryParameter("k", "email")
                    .appendQueryParameter("v", email)
            try {
                val (resp, _) = withContext(Dispatchers.IO) {
                    Fetch()
                            .get(apiUrl.toString())
                            .responseApi()
                }

                if (resp.code() == 204) {
                    _emailResult.value = FindEmailResult(
                            success = Pair(email, true)
                    )
                } else {
                    _emailResult.value = FindEmailResult(
                            exception = Exception("API error ${resp.code()}")
                    )
                }

            } catch (e: ClientError) {

                if (e.statusCode== 404) {
                    _emailResult.value = FindEmailResult(
                            success = Pair(email, false)
                    )

                    return@launch
                }

                _emailResult.value = FindEmailResult(
                        error = e.statusMessage()
                )

            } catch (e:Exception) {

                _emailResult.value = FindEmailResult(
                        exception = e
                )
            }
        }
    }

    fun login(c: Credentials) {

        viewModelScope.launch {
            try {
                val userId = withContext(Dispatchers.IO) {
                    c.login()
                }

                if (userId == null) {

                    _loginResult.value = AccountResult(success = null)

                    return@launch
                }

                loadFtcAccount(userId)
            } catch (e: ClientError) {
                val msgId = if (e.statusCode == 404) {
                    R.string.error_invalid_password
                } else {
                    e.statusMessage()
                }

                _loginResult.value = AccountResult(
                        error = msgId,
                        exception = e
                )

            } catch (e: Exception) {

                _loginResult.value = AccountResult(
                        exception = e
                )
            }
        }
    }

    /**
     * Uses wechat authrozation code to get an access token, and then use the
     * token to get user info.
     * API responds with WxSession data to uniquely identify this login
     * session.
     * You can user the session data later to retrieve user account.
     */
    fun wxLogin(code: String, isManualRefresh: Boolean) {
        viewModelScope.launch {
            try {
                val sess = withContext(Dispatchers.IO) {
                    WxOAuth.login(code)
                }

                _wxOAuthResult.value = WxOAuthResult(
                        success = sess
                )

                // If user is manually refreshing
                // WxFragment and refresh token is
                // expired, do not retrieve latest
                // account data because user's
                // current login method might be
                // email, not wechat.
                if (isManualRefresh) {
                    return@launch
                }

                if (sess == null) {
                    return@launch
                }

                // Fetch account data from wechat side
                // if the authorization is used for initial login,
                // or triggered by refresh token
                // expiration mechanism.
                // The expiration detection mechanism is only triggered if user logged in
                // via wechat only.
                loadWxAccount(sess)
            } catch (e: ClientError) {
                // Possible 422 error key: code_missing_field, code_invalid.
                // We cannot make sure the exact meaning of each error, just
                // show user API's error message.
                _wxOAuthResult.value = WxOAuthResult(
                        error = when (e.statusCode) {
                            422 -> null
                            else -> e.statusMessage()
                        },
                        exception = e
                )
            } catch (e: Exception) {
                _wxOAuthResult.value = WxOAuthResult(
                        exception = e
                )
            }
        }
    }

    /**
     * Handles both a new user signup, or wechat-logged-in
     * user trying to link to a new account.
     */
    fun signUp(c: Credentials, wxSession: WxSession? = null) {

        viewModelScope.launch {
            try {
                val userId = withContext(Dispatchers.IO) {
                    c.signUp(wxSession?.unionId)
                }

                if (userId == null) {
                    _loginResult.value = AccountResult()
                    return@launch
                }


                if (wxSession == null) {
                    loadFtcAccount(userId)
                    return@launch
                }

                loadWxAccount(wxSession)

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
                    e.statusMessage()
                }

                _loginResult.value = AccountResult(
                        error = msgId,
                        exception = e
                )

            } catch (e: Exception) {

                _loginResult.value = AccountResult(exception = e)
            }
        }
    }

    /**
     * Load account after user performed wechat authorization
     */
    private suspend fun loadWxAccount(wxSession: WxSession) {
        try {
            val account = withContext(Dispatchers.IO) {
                wxSession.fetchAccount()
            }

            _loginResult.value = AccountResult(success = account)
        } catch (e: ClientError) {
            val msgId = if (e.statusCode == 404) {
                R.string.error_not_loaded
            } else {
                e.statusMessage()
            }

            _loginResult.value = AccountResult(
                    error = msgId,
                    exception = e
            )

        } catch (e: Exception) {

            _loginResult.value = AccountResult(exception = e)
        }
    }

    /**
     * Load account after user's password verified
     * or signed up.
     */
    private suspend fun loadFtcAccount(userId: String) {
        try {
            val account = withContext(Dispatchers.IO) {
                FtcUser(id = userId).fetchAccount()
            }

            _loginResult.value = AccountResult(success = account)

        } catch (e: ClientError) {
            val msgId = if (e.statusCode == 404) {
                R.string.error_not_loaded
            } else {
                e.statusMessage()
            }

            _loginResult.value = AccountResult(
                    error = msgId,
                    exception = e
            )

        } catch (e: Exception) {

            _loginResult.value = AccountResult(exception = e)
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

    fun showProgress(show: Boolean) {
        inProgress.value = show
    }

    fun enableInput(enable: Boolean) {
        inputEnabled.value = enable
    }
}
