package com.ft.ftchinese.model.ftcsubs

import com.ft.ftchinese.model.reader.Membership

data class ConfirmationResult (
        val order: Order,
        val membership: Membership
)
