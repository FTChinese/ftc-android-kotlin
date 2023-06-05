package com.ft.ftchinese.model.enums

import androidx.annotation.StringRes
import com.ft.ftchinese.R

enum class FontSize(
    val key: String,
    val size: Int,
    @StringRes val fontSizeId: Int
) {
    Smallest("smallest", 12, R.string.font_size_smallest),
    Smaller("smaller", 14, R.string.font_size_smaller),
    Normal("normal", 16, R.string.font_size_normal),
    Bigger("bigger", 18, R.string.font_size_bigger),
    Biggest("biggest", 20, R.string.font_size_biggest);

    companion object {
        private val map = FontSize.values().associateBy(FontSize::key)

        fun fromKey(k: String) = map[k]
    }
}
