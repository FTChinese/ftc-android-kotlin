package com.ft.ftchinese

import com.ft.ftchinese.models.Account
import com.ft.ftchinese.models.User
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.gson
import org.junit.Test

class RequestTest {
    @Test
    fun loginTest() {
        val account = Account(email = "weiguo.ni@ftchinese.com", password = "12345678")

        val response = Fetch.post("http://localhost:8000/users/auth", gson.toJson(account))

        System.out.println("Response code: ${response.code()}")
        System.out.println("Response message: ${response.message()}")

        System.out.println("Is successful: ${response.isSuccessful}")

        val responseBody = response.body()?.string()
        System.out.println("Response body: $responseBody")

        if (responseBody != null) {
            val user = gson.fromJson<User>(responseBody, User::class.java)

            System.out.println(user)
        }

    }
}