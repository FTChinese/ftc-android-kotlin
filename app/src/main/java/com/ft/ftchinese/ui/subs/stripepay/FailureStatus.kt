package com.ft.ftchinese.ui.subs.stripepay

sealed class FailureStatus {
    class Message(val message: String) : FailureStatus()
    class NextAction(val secret: String) : FailureStatus()
}

