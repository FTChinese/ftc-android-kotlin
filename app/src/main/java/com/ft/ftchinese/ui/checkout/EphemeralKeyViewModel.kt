package com.ft.ftchinese.ui.checkout

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.isConnected
import com.stripe.android.EphemeralKeyProvider
import com.stripe.android.EphemeralKeyUpdateListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "EphemeralKeyViewModel"

class EphemeralKeyViewModel(application: Application) : AndroidViewModel(application), EphemeralKeyProvider {
    private val session = SessionManager.getInstance(application)
    val isNetworkAvailable = MutableLiveData(application.isConnected)

    override fun createEphemeralKey(
        apiVersion: String,
        keyUpdateListener: EphemeralKeyUpdateListener
    ) {
        if (isNetworkAvailable.value == false) {
            keyUpdateListener.onKeyUpdateFailure(400, "Network unavailable")
            return
        }

        val account = session.loadAccount() ?: return

        viewModelScope.launch {
            try {
                val rawKey = withContext(Dispatchers.IO) {
                    StripeClient.createEphemeralKey(account, apiVersion)
                }


                // Example
                // {
                //  "id": "ephkey_1EsLbEBzTK0hABgJoDVLylpw",
                //  "object": "ephemeral_key",
                //  "associated_objects": [
                //    {
                //      "type": "customer",
                //      "id": "cus_FMPLOeckvP32Fo"
                //    }
                //  ],
                //  "created": 1562210828,
                //  "expires": 1562214428,
                //  "livemode": false,
                //  "secret": "ek_test_***aq5O"
                //}
                // The key lasts 1 hour.

                if (rawKey != null) {
                    keyUpdateListener.onKeyUpdate(rawKey)
                    return@launch
                }

                keyUpdateListener.onKeyUpdateFailure(400, "Empty raw key")

            } catch (e: APIError) {
                // ClientError(
                // message=No such customer: 'cus_abc',
                // error=null,
                // statusCode=400,
                // code=resource_missing,
                // param=customer,
                // type=invalid_request_error)
                Log.i(TAG, "$e")
                if (e.code == "resource_missing" && e.param == "customer") {
                    keyUpdateListener.onKeyUpdateFailure(e.statusCode, "Error: Stripe customer not found!")
                } else {
                    keyUpdateListener.onKeyUpdateFailure(e.statusCode, e.message)
                }

            } catch (e: Exception) {
                Log.i(TAG, "$e")
                keyUpdateListener.onKeyUpdateFailure(500, e.localizedMessage ?: "")
            }
        }
    }
}
