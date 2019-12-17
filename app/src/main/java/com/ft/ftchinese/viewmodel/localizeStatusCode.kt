package com.ft.ftchinese.viewmodel

import com.ft.ftchinese.R

val statusCodes = mapOf(
        401 to R.string.api_unauthorized,
        429 to R.string.api_too_many_request,
        500 to R.string.api_server_error
)
