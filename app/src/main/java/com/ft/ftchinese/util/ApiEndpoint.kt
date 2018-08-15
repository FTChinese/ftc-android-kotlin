package com.ft.ftchinese.util

class ApiEndpoint {
    companion object {
        private const val BASE = "http://465c6e34.ngrok.io"
        const val LOGIN = "$BASE/users/auth"
        const val NEW_ACCOUNT = "$BASE/users/new"
        const val PROFILE = "$BASE/user/profile"
    }
}