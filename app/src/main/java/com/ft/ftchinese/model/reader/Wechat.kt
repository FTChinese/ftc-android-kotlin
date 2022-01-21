package com.ft.ftchinese.model.reader

import android.os.Parcelable
import com.beust.klaxon.Json
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * Example Wechat avatar url:
 * http://thirdwx.qlogo.cn/mmopen/vi_32/Q0j4TwGTfTLB34sBwSiaL3GJmejqDUqJw4CZ8Qs0ztibsRu6wzMpg7jg5icxWKwxF73ssZUmXmee1MvSvaZ6iaqs1A/132
 */
@Parcelize
data class Wechat(
    @Json("nickname")
    val nickname: String? = null,
    @Json("avatarUrl")
    val avatarUrl: String? = null
): Parcelable {

    @Json(ignored = true)
    @IgnoredOnParcel
    val isEmpty: Boolean
        get() = nickname.isNullOrBlank() && avatarUrl.isNullOrBlank()
}
