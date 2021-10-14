package com.ft.ftchinese.service

import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.model.fetch.APIError
import com.stripe.android.EphemeralKeyProvider
import com.stripe.android.EphemeralKeyUpdateListener
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class StripeEphemeralKeyProvider(
        private val account: Account
) : EphemeralKeyProvider, AnkoLogger {

    private var job: Job? = null

    override fun createEphemeralKey(apiVersion: String, keyUpdateListener: EphemeralKeyUpdateListener) {

        info("API version: $apiVersion")

        job = GlobalScope.launch(Dispatchers.Main) {
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

                keyUpdateListener.onKeyUpdateFailure(200, "Empty raw key")

            } catch (e: APIError) {
                // ClientError(
                // message=No such customer: 'cus_abc',
                // error=null,
                // statusCode=400,
                // code=resource_missing,
                // param=customer,
                // type=invalid_request_error)
                info(e)
                if (e.code == "resource_missing" && e.param == "customer") {
                    keyUpdateListener.onKeyUpdateFailure(e.statusCode, "Error: Stripe customer not found!")
                } else {
                    keyUpdateListener.onKeyUpdateFailure(e.statusCode, e.message)
                }

            } catch (e: Exception) {
                info(e)
                keyUpdateListener.onKeyUpdateFailure(500, e.localizedMessage ?: "")
            }
        }
    }

    fun onDestroy() {
        job?.cancel()
    }
}
