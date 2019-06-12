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
import com.ft.ftchinese.model.WxSession
import com.ft.ftchinese.util.ClientError
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.NextApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginViewModel : ViewModel() {

    val email = MutableLiveData<Pair<String, Boolean>>()
    val userId = MutableLiveData<String>()


    val inProgress = MutableLiveData<Boolean>()
    val input = MutableLiveData<Boolean>()

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> =_loginForm

    private val _emailResult = MutableLiveData<FindEmailResult>()
    val emailResult: LiveData<FindEmailResult> =_emailResult

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

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
        inProgress.value = true

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

                inProgress.value = false

                if (resp.code() == 204) {
                    _emailResult.value = FindEmailResult(success = Pair(email, true))
                } else {
                    _emailResult.value = FindEmailResult(error = Exception("API error ${resp.code()}"))
                }

            } catch (e: ClientError) {
                inProgress.value = false

                if (e.statusCode== 404) {
                    _emailResult.value = FindEmailResult(success = Pair(email, false))

                    return@launch
                }

                _emailResult.value = FindEmailResult(error = e)

                input.value = true

            } catch (e:Exception) {
                inProgress.value = false

                _emailResult.value = FindEmailResult(error = e)

                input.value= true
            }
        }
    }



    fun login(c: Credentials) {
        inProgress.value = true

        viewModelScope.launch {
            try {
                val userId = withContext(Dispatchers.IO) {
                    c.login()
                }

                if (userId == null) {
                    inProgress.value = false
                    input.value = true

                    _loginResult.value = LoginResult(success = null)

                    return@launch
                }

                loadAccount(userId)
            } catch (e: ClientError) {
                inProgress.value = false
                input.value = true

                _loginResult.value = if (e.statusCode == 404)
                    LoginResult(error = R.string.error_invalid_password)
                else
                    LoginResult(exception = e)
            } catch (e: Exception) {
                inProgress.value = false
                input.value = true

                _loginResult.value = LoginResult(exception = e)
            }
        }
    }

    fun signUp(c: Credentials, wxSession: WxSession? = null) {
        inProgress.value = true
        viewModelScope.launch {
            try {
                val userId = withContext(Dispatchers.IO) {
                    c.signUp(wxSession?.unionId)
                }

                if (userId == null) {
                    inProgress.value = false
                    input.value = true

                    _loginResult.value = LoginResult(error = R.string.prompt_sign_up_failed)

                    return@launch
                }

                // TODO: initial singup of wechat linking to new account
                // based on WxSession
                loadAccount(userId)

            } catch (e: Exception) {
                inProgress.value = false
                input.value = true

                _loginResult.value = LoginResult(exception = e)
            }
        }
    }

    private suspend fun loadAccount(userId: String) {
        try {
            val account = withContext(Dispatchers.IO) {
                FtcUser(id = userId).fetchAccount()
            }

            inProgress.value = false

            _loginResult.value = LoginResult(success = account)

        } catch (e: ClientError) {
            inProgress.value = false
            input.value = true

            _loginResult.value = if (e.statusCode == 404)
                LoginResult(error = R.string.error_not_loaded)
            else
                LoginResult(exception = e)

        } catch (e: Exception) {
            inProgress.value = false
            input.value = true

            _loginResult.value = LoginResult(exception = e)
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

    fun setUserId(id: String) {
        userId.value = id
    }


}
