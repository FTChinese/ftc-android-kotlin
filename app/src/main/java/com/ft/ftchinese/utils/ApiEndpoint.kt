package com.ft.ftchinese.utils

class ApiEndpoint {
    companion object {
        private const val BASE = "http://c06e62eb.ngrok.io"
        const val LOGIN = "$BASE/users/auth"
        const val NEW_ACCOUNT = "$BASE/users/new"
    }
}