package com.ft.ftchinese.model

/**
 * Permission uses bitwise operation represent an article's
 * access right.
 * An article should always have a single value of the enum,
 * while user's access should contain the combination of them.
 */
enum class Permission(val id: Int) {
    FREE(1), // 0001
    STANDARD(2), // 0010
    PREMIUM(4) // 0100
}
