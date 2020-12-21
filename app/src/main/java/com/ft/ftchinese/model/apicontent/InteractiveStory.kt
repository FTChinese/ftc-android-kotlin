package com.ft.ftchinese.model.apicontent

import com.ft.ftchinese.model.fetch.KDateTime
import org.threeten.bp.ZonedDateTime

data class Timeline(
        val start: Double? = null,
        val text: String
)

data class Word(
    val term: String,
    val description: String
)

data class InteractiveStory(
    val id: String,
    val type: String,
    @KDateTime
    val createdAt: ZonedDateTime? = null,

    @KDateTime
    val updatedAt: ZonedDateTime? = null,

    val title: String,
    val standfirst: String,
    val coverUrl: String,
    val tags: List<String>,
    val audioUrl: String? = null,

    val bodyXml: String,
    val alternativeTitles: AlternativeTitles,
    val timeline: List<List<Timeline>>? = null,
    val vocabularies: List<Word>? = null,
    val quiz: String? = null
) {
    fun lyrics(): List<String> {

        val headline = if (alternativeTitles.english != null) {
            listOf(alternativeTitles.english)
        } else {
            listOf()
        }

        if (timeline != null) {
            return headline + timeline.flatMap {
                it.map { timeline: Timeline ->
                    timeline.text
                }
            }
        }

        return headline + bodyXml.split("\n")
    }
}
