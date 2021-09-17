package com.ft.ftchinese.ui.dialog

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DialogArgs (
    val message: String,
    val positiveButton: Int,
    val negativeButton: Int? = null,
    val title: Int? = null,
) : Parcelable
