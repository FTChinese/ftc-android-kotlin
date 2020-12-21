package com.ft.ftchinese.service

import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.model.fetch.ClientError
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

        job = GlobalScope.launch {
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
                info("rawKey: $rawKey")

                if (rawKey != null) {
                    keyUpdateListener.onKeyUpdate(rawKey)
                    return@launch
                }

                keyUpdateListener.onKeyUpdateFailure(200, "Empty raw key")

            } catch (e: ClientError) {
                keyUpdateListener.onKeyUpdateFailure(e.statusCode, e.message)

            } catch (e: Exception) {
                keyUpdateListener.onKeyUpdateFailure(500, e.localizedMessage)
            }
        }
    }

    fun onDestroy() {
        job?.cancel()
    }
}
