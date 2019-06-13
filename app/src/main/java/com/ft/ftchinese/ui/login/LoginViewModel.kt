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
    val inputEnabled = MutableLiveData<Boolean>()

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
                    _emailResult.value = FindEmailResult(success = Pair(email, true))
                } else {
                    _emailResult.value = FindEmailResult(error = Exception("API error ${resp.code()}"))
                }

            } catch (e: ClientError) {

                if (e.statusCode== 404) {
                    _emailResult.value = FindEmailResult(success = Pair(email, false))

                    return@launch
                }

                _emailResult.value = FindEmailResult(error = e)


            } catch (e:Exception) {

                _emailResult.value = FindEmailResult(error = e)
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

                    _loginResult.value = LoginResult(success = null)

                    return@launch
                }

                loadAccount(userId)
            } catch (e: ClientError) {

                _loginResult.value = if (e.statusCode == 404)
                    LoginResult(error = R.string.error_invalid_password)
                else
                    LoginResult(exception = e)
            } catch (e: Exception) {

                _loginResult.value = LoginResult(exception = e)
            }
        }
    }

    fun signUp(c: Credentials, wxSession: WxSession? = null) {

        viewModelScope.launch {
            try {
                val userId = withContext(Dispatchers.IO) {
                    c.signUp(wxSession?.unionId)
                }

                if (userId == null) {

                    _loginResult.value = LoginResult(error = R.string.prompt_sign_up_failed)

                    return@launch
                }


                if (wxSession == null) {
                    loadAccount(userId)
                    return@launch
                }

                loadWxAccount(wxSession)

            } catch (e: Exception) {

                _loginResult.value = LoginResult(exception = e)
            }
        }
    }

    private suspend fun loadWxAccount(wxSession: WxSession) {
        try {
            val account = withContext(Dispatchers.IO) {
                wxSession.fetchAccount()
            }

            _loginResult.value = LoginResult(success = account)
        } catch (e: ClientError) {

            _loginResult.value = if (e.statusCode == 404)
                LoginResult(error = R.string.error_not_loaded)
            else
                LoginResult(exception = e)
        } catch (e: Exception) {

            _loginResult.value = LoginResult(exception = e)
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
            inputEnabled.value = true

            _loginResult.value = if (e.statusCode == 404)
                LoginResult(error = R.string.error_not_loaded)
            else
                LoginResult(exception = e)

        } catch (e: Exception) {
            inputEnabled.value = true

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

    fun enableInput(enable: Boolean) {
        inputEnabled.value = enable
    }

    fun setUserId(id: String) {
        userId.value = id
    }


}
