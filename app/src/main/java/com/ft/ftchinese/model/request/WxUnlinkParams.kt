package com.ft.ftchinese.model.request

import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.reader.UnlinkAnchor

data class WxUnlinkParams(
    val ftcId: String,
    val anchor: UnlinkAnchor? = null,
) {
    fun toJsonString(): String {
        return json.toJsonString(this)
    }
}
