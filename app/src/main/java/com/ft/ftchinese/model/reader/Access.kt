package com.ft.ftchinese.model.reader

import android.os.Parcelable
import com.ft.ftchinese.model.content.Language
import kotlinx.parcelize.Parcelize

// Configuration of permission denied fragment.
@Parcelize
data class Access(
    val status: MemberStatus,
    val rights: Int, // User owned permissions. Sum of multiple binary.
    val content: Permission, // Content allowed permission.
    val lang: Language,
) : Parcelable {
    // Check if content's permission is contained by user rights.
    val granted: Boolean
        get() = content.grant(rights)

    val loggedIn: Boolean
        get() = status != MemberStatus.NotLoggedIn

    val cancellable: Boolean
        get() = lang != Language.CHINESE

    val isBilingual: Boolean
        get() = lang != Language.CHINESE

    companion object {
        // Deduce user's access permissions against
        // content's permission.
        @JvmStatic
        fun of(
            contentPerm: Permission,
            who: Account?,
            lang: Language = Language.CHINESE
        ): Access {
            if (who == null) {
                return Access(
                    status = MemberStatus.NotLoggedIn,
                    rights = Permission.FREE.id,
                    content = contentPerm,
                    lang = lang,
                )
            }

            val (rights, status) = who.membership
                .accessRights()

            return Access(
                status = status,
                rights = rights,
                content = contentPerm,
                lang = lang
            )
        }

        fun ofEnglishArticle(who: Account?, lang: Language): Access {
            return of(Permission.STANDARD, who, lang = lang)
        }
    }
}
