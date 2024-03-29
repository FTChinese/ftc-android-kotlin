package com.ft.ftchinese.model.reader

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Example Wechat avatar url:
 * http://thirdwx.qlogo.cn/mmopen/vi_32/Q0j4TwGTfTLB34sBwSiaL3GJmejqDUqJw4CZ8Qs0ztibsRu6wzMpg7jg5icxWKwxF73ssZUmXmee1MvSvaZ6iaqs1A/132
 */
@Parcelize
@Serializable
data class Wechat(
    val nickname: String? = null,
    val avatarUrl: String? = null
): Parcelable {

    @IgnoredOnParcel
    val isEmpty: Boolean
        get() = nickname.isNullOrBlank() && avatarUrl.isNullOrBlank()
}
