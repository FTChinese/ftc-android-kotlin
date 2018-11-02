package com.ft.ftchinese

import com.ft.ftchinese.models.*
import com.ft.ftchinese.util.Fetch
import com.github.javafaker.Faker
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okhttp3.Response
import org.joda.time.LocalDate
import org.joda.time.format.ISODateTimeFormat
import org.junit.Test

class RequestTest {
    @Test
    fun loginTest() {
        val account = Login(email = "weiguo.ni@ftchinese.com", password = "12345678")

        val response = Fetch()
                .post("http://localhost:8000/users/auth")
                .setClient()
                .noCache()
                .body(account)
                .end()

        System.out.println("Response code: ${response.code()}")
        System.out.println("Response message: ${response.message()}")

        System.out.println("Is successful: ${response.isSuccessful}")

        val responseBody = response.body()?.string()
        System.out.println("Response body: $responseBody")
    }

    @Test
    fun signUp() {
        val faker = Faker()

        val email = faker.internet().emailAddress()
        val account = Login(email = email, password = "12345678")

        val response = Fetch()
                .post("http://localhost:8000/users/new")
                .setClient()
                .noCache()
                .body(account)
                .end()

       print(response)

    }

    @Test
    fun changeEmail() {
        val response = Fetch()
                .patch("http://localhost:8000/user/email")
                .noCache()
                .setUserId("e1a1f5c0-0e23-11e8-aa75-977ba2bcc6ae")
                .body(EmailUpdate("weiguo.ni@ftchinese.com"))
                .end()

        print(response)
    }

    @Test
    fun changeName() {
        val response = Fetch()
                .patch("http://localhost:8000/user/name")
                .noCache()
                .setUserId("e1a1f5c0-0e23-11e8-aa75-977ba2bcc6ae")
                .body(UserNameUpdate("Victor Nee"))
                .end()

        print(response)
    }

    @Test
    fun ad() = runBlocking {

        val prefSchedule = mutableMapOf<String, MutableSet<LaunchAd>>()

        val schedule = async {
            LaunchSchedule.fetchData()
        }.await() ?: return@runBlocking

        val today = LocalDate.now()

        for (item in schedule.sections) {
            if (item.android != "yes") {
                continue
            }

            for (date in item.scheduledOn) {
                if (date.isNullOrBlank()) {
                    continue
                }
                try {
                    val planned = LocalDate.parse(date, ISODateTimeFormat.basicDate())
                    if (today.isAfter(planned)) {
                        continue
                    }

                    if (prefSchedule.containsKey(date)) {
                        prefSchedule[date]?.add(item)
                    } else {
                        prefSchedule[date] = mutableSetOf(item)
                    }

                } catch (e: Exception) {
                    continue
                }
            }
        }

        for ((key, value) in prefSchedule) {
            System.out.println(key)
            System.out.println(value)
        }
    }

    @Test fun ftIntelligence() {
        val htmlStr = Fetch().get("http://www.ftchinese.com/m/marketing/intelligence.html?webview=ftcapp&001").string()

        println(htmlStr)
    }
}

fun print(response: Response) {
    System.out.println("Response code: ${response.code()}")
    System.out.println("Response message: ${response.message()}")

    if (response.isSuccessful) {
        System.out.println("Response body: ${response.body()?.string()}")
    }
}