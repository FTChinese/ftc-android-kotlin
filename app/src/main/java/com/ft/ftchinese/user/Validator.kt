package com.ft.ftchinese.user

fun isEmailValid(email: String): Boolean {
    return email.contains("@")
}

fun isPasswordValid(password: String): Boolean {
    return password.length > 4
}