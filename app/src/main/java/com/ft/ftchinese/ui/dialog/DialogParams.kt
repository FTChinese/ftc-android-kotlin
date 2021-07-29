package com.ft.ftchinese.ui.dialog

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DialogParams (
    val positive: String,
    val negative: String? = null,
    val message: String,
) : Parcelable
