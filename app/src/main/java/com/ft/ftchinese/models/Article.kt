package com.ft.ftchinese.models

import android.content.Context
import android.text.format.DateFormat

data class Bilingual(
        var cn: String,
        var en: String?
)

data class StoryPic(
        val smallbutton: String,
        val other: String
)

data class RelatedStory(
        val id: String,
        val cheadline: String,
        val eheadline: String,
        val last_publish_time: String
)

class Story (
        val id: String,
        val fileupdatetime: String,
        val cheadline: String,
        val clongleadbody: String,
        val cbyline_description: String,
        val cauthor: String,
        val cbyline_status: String,
        val cbody: String,
        val eheadline: String,
        val ebyline_description: String,
        val eauthor: String,
        val ebyline_status: String,
        val elongleadbody: String,
        val ebyline: String,
        val ebody: String,
        val tag: String,
        val genre: String,
        val topic: String,
        val area: String,
        val last_publish_time: String,
        val story_pic: StoryPic,
        val relative_story: Array<RelatedStory>
) {
    private val TAG = "Story"

    val title: Bilingual
        get() = Bilingual(cheadline, eheadline)

    val isBilingual: Boolean
        get() = ebody.isNotBlank()

    val standfirst: String
        get() = clongleadbody

    val byline: String
        get() = "$cbyline_description $cauthor $cbyline_status"

    val bodyXML: Bilingual
        get() = Bilingual(cbody, ebody)

    val bodyAlignedXML: String
        get() {
            val alignedBody = alignBody()

            return alignedBody.joinToString("") {
                "${it.cn}${it.en}"
            }
        }

    val tags: List<String>
        get() = tag.split(",")

    val keywords: String
        get() = "$tag,$area,$topic,$genre"
                .replace(Regex(",+"), ",")
                .replace(Regex("^,"), "")
                .replace(Regex(",$"), "")

    val updatedAt: String
        get() = DateFormat.format("yyyy年M月d日 HH:mm", fileupdatetime.toLong() * 1000) as String

    val createdAt: String
        get() = DateFormat.format("yyyy年M月d日 HH:mm", last_publish_time.toLong() * 1000) as String

    fun htmlForRelatedStories(): String {

        val listItems = relative_story.mapIndexed { index, relatedStory ->
            """
            <li class="mp${index+1}"><a target="_blank" href="/story/${relatedStory.id}\">${relatedStory.cheadline}</a></li>
            """.trimIndent()
        }
                .joinToString("")

        return """
            <div class="story-box">
                <h2 class="box-title"><a>相关文章</a></h2>
                <ul class="top10">$listItems</ul>
            </div>
        """.trimIndent()
    }

    fun htmlForRelatedTopics(): String {
        return tags.mapIndexed { index, s ->
            """
            <li class="story-theme mp${index+1}">
                <a target="_blank" href="/tag/$s\">$s</a>
                <div class="icon-right">
                    <button class="myft-follow plus" data-tag="$s" data-tier="tag">关注</button>
                </div>
            </li>
            """.trimIndent()
        }
                .joinToString("")

    }

    fun htmlForCoverImage(): String {
        return """
            <div class="story-image image" style="margin-bottom:0;">
                <figure data-url="${story_pic.smallbutton}" class="loading"></figure>
            </div>
        """.trimIndent()
    }

    fun htmlForTheme(): String {
        val firstTag = tags[0]

        return """
            <div class="story-theme">
                <a target="_blank" href="/tag/$firstTag">$firstTag</a>
                <button class="myft-follow plus" data-tag="${firstTag}" data-tier="tag">关注</button>
            </div>
        """.trimIndent()
    }

    fun getCnBody(withAd: Boolean = true): String {
        if (!withAd) {
            return cbody
        }

        val arrBody = splitCnBody().toMutableList()

        // Insert ad after paragraph 3 and paragraph 9 of the original list.
        if (arrBody.size > 3) {
            arrBody.add(3, AdParser.getAdCode(AdPosition.MIDDLE_ONE))
        } else {
            arrBody.add(arrBody.size, AdParser.getAdCode(AdPosition.MIDDLE_ONE))
        }

        if (arrBody.size > 10) {
            arrBody.add(10, AdParser.getAdCode(AdPosition.MIDDLE_TWO))
        }

        return arrBody.joinToString("")
    }

    fun getEnBody(withAd: Boolean = true): String {
        if (ebody.isNullOrBlank()) {
            return ""
        }

        if (!withAd) {
            return ebody
        }

        val arrBody = splitEnBody().toMutableList()

        // Insert ad after paragraph 3 and paragraph 9 of the original list.
        if (arrBody.size > 3) {
            arrBody.add(3, AdParser.getAdCode(AdPosition.MIDDLE_ONE))
        } else {
            arrBody.add(arrBody.size, AdParser.getAdCode(AdPosition.MIDDLE_ONE))
        }

        if (arrBody.size > 10) {
            arrBody.add(10, AdParser.getAdCode(AdPosition.MIDDLE_TWO))
        }

        return arrBody.joinToString("")
    }

    fun getBilingualBody(): String {
        if (ebody.isNullOrBlank()) {
            return ""
        }

        val alignedBody = alignBody()

        return alignedBody.joinToString("") {
            "${it.cn}${it.en}"
        }
    }

    private fun splitCnBody(): List<String> {
        return cbody.split("\r\n")
    }

    private fun splitEnBody(): List<String> {
        return ebody.split("\r\n")
    }

    /**
     * Align english text to chinese text paragraph by paragraph.
     */
    private fun alignBody(): List<Bilingual> {
        val cnArray = cbody.split("\r\n")
        val enArray = ebody.split("\r\n")

        val bi = mutableListOf<Bilingual>()

        var cIndex = 0
        var eIndex = 0

        val cLen = cnArray.size
        val eLen = enArray.size


        while (cIndex < cLen && eIndex < eLen) {
            val pair = Bilingual(cn = cnArray[cIndex], en = enArray[eIndex])
            bi.add(pair)
            cIndex++
            eIndex++
        }

        while (cIndex < cLen) {
            val pair = Bilingual(cn = cnArray[cIndex], en = "")
            bi.add(pair)
            cIndex++
        }

        while (eIndex < eLen) {
            val pair = Bilingual(cn = "", en = enArray[eIndex])
            bi.add(pair)
            eIndex++
        }

        return bi
    }
}