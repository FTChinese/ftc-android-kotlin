package com.ft.ftchinese.model.reader

data class Access(
    val status: MemberStatus,
    val rights: Int, // User owned permissions. Sum of multiple binary.
    val content: Permission, // Content allowed permission.
) {
    // Check if content's permission is contained by user rights.
    val granted: Boolean
        get() = content.grant(rights)

    companion object {
        // Deduce user's access permissions against
        // content's permission.
        @JvmStatic
        fun of(contentPerm: Permission, who: Account?): Access {
            if (who == null) {
                return Access(
                    status = MemberStatus.NotLoggedIn,
                    rights = Permission.FREE.id,
                    content = contentPerm,
                )
            }

            val (rights, status) = who.membership
                .accessRights()

            return Access(
                status = status,
                rights = rights,
                content = contentPerm,
            )
        }

        fun ofEnglishArticle(who: Account?): Access {
            return of(Permission.STANDARD, who)
        }
    }
}
