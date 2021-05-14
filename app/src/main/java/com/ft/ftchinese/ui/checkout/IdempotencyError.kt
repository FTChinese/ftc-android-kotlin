package com.ft.ftchinese.ui.checkout

import java.lang.Exception

data class IdempotencyError(
        override val message: String? = "problem with idempotency key"
) : Exception(message)

