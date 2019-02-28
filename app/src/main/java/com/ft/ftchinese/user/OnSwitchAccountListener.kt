package com.ft.ftchinese.user

/**
 * Used by WxAccountFragment and FtcAccountFragment to tell
 * AccountActivity that accounts binding is changed.
 */
interface OnSwitchAccountListener {
    fun onProgress(show: Boolean)

    fun onSwitchAccount()
}