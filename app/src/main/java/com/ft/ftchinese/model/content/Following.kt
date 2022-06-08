package com.ft.ftchinese.model.content

import kotlinx.serialization.Serializable
import java.net.URLEncoder



/**
 * Used to parse messages passed from JS when user clicked `FOLLOW` button.
 * Those data are stored locally.
 * Example saved XML:
 * ```xml
 * <map>
 *  <set name="tag">
 *      <string>美国</string>
 *      <string>中国经济</string>
 *      <string>特朗普</string>
 *   </set>
 * </map>
 * ```
 */
@Serializable
data class Following(
        var type: String, // JS uses this value. Possible values: `tag`, `topic`, `industry`, `area`, `author`, `column`.
        var tag: String, // This is the string shown along with the FOLLOW button
        var action: String // `follow` or `unfollow`. Used to determine if user followed or unfollowed something.
) {
    val topic: String
        get() = URLEncoder.encode("${type}_$tag", "utf-8")
}

