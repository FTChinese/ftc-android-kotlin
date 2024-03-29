package com.ft.ftchinese.model.reader

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Address(
    val country: String? = null,
    val province: String? = null,
    val city: String? = null,
    val district: String? = null,
    val street: String? = null,
    val postcode: String? = null,
) {

    fun toJsonString(): String {
        return Json.encodeToString(this)
    }

    fun withProvince(p: String): Address {
        return Address(
            country = country,
            province = p,
            city = city,
            district = district,
            street = street,
            postcode = postcode
        )
    }

    fun withCity(c: String): Address {
        return Address(
            country = country,
            province = province,
            city = c,
            district = district,
            street = street,
            postcode = postcode
        )
    }

    fun withDistrict(d: String): Address {
        return Address(
            country = country,
            province = province,
            city = city,
            district = d,
            street = street,
            postcode = postcode
        )
    }

    fun withStreet(s: String): Address {
        return Address(
            country = country,
            province = province,
            city = city,
            district = district,
            street = s,
            postcode = postcode
        )
    }

    fun withPostcode(code: String): Address {
        return Address(
            country = country,
            province = province,
            city = city,
            district = district,
            street = street,
            postcode = code,
        )
    }
}
