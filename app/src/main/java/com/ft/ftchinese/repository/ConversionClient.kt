package com.ft.ftchinese.repository

import com.ft.ftchinese.model.conversion.*
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.HttpResp

object ConversionClient {

    fun getConversion(
        ua: ConversionTrackingUA,
        params: ConversionTrackingRequest,
        timeout: Int,
    ): HttpResp<ConversionTrackingResponse> {
        return Fetch()
            .post(Endpoint.conversionTracking)
            .addParams(params.toMap())
            .addHeader("User-Agent", ua.toString())
            .send()
            .setTimeout(timeout)
            .endJson()
    }

    fun listCampaigns(): FtcCampaigns? {
        return Fetch()
            .get(Endpoint.ftcCampaign)
            .endJson<FtcCampaigns>()
            .body
    }
}
