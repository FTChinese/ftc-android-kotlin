package com.ft.ftchinese.model.apicontent

import com.ft.ftchinese.model.subscription.Tier
import com.ft.ftchinese.util.KDateTime
import com.ft.ftchinese.util.KTier
import org.threeten.bp.ZonedDateTime

data class Related (
    val id: String,
    val type: String,
    @KDateTime
    val createdAt: ZonedDateTime? = null,

    @KDateTime
    val updatedAt: ZonedDateTime? = null,

    @KTier
    val tier: Tier? = null,

    val title: String
)

data class BilingualStory(
        val id: String,
        val type: String,

        @KDateTime
        val createdAt: ZonedDateTime? = null,

        @KDateTime
        val updatedAt: ZonedDateTime? = null,
        @KTier
        val tier: Tier? = null,

        val title: String,

        val standfirst: String,
        val coverUrl: String,
        val tags: List<String>,
        val audioUrl: String? = null,

        val bilingual: Boolean,
        val byline: Byline,
        val areas: List<String>,
        val genres: List<String>,
        val industries: List<String>,
        val topics: List<String>,
        val body: List<BilingualPara>,
        val translator: String? = null,
        val alternativeTitles: AlternativeTitles,
        val related: List<Related>
) {
    fun lyrics(): List<String> {

        return if (alternativeTitles.english != null) {
            listOf(alternativeTitles.english) + body.map {
                it.en
            }
        } else {
            body.map {
                it.en
            }
        }
    }
}
