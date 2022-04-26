package com.ft.ftchinese.repository

// A wrapper indicating from where data is loaded.
data class LoadedSource<T>(
    val data: T,
    val isRemote: Boolean,
)
