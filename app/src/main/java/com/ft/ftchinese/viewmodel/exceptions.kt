package com.ft.ftchinese.viewmodel

import java.lang.Exception

data class IdempotencyError(
        override val message: String? = "problem with idempotency key"
) : Exception(message)

