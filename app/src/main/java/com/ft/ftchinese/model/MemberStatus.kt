package com.ft.ftchinese.model

/**
 * The id is used to calculate multiple status.
 * For example, if the membership is both renewable and upgradable,
 * we can perform bitwise `or`: RENEWABLE | UPGRADABLE = 1010.
 * Since each value takes different position in the binary number,
 * any combination is always unique.
 */
enum class MemberStatus(val id: Int) {
    INVALID(1),
    EXPIRED(2),
    RENEWABLE(4),
    BEYOND_RENEW(8),
    UPGRADABLE(16)
}
