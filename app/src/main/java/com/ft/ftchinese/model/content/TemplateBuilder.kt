package com.ft.ftchinese.model.content

import android.util.Log
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Address
import com.ft.ftchinese.tracking.AdParser
import com.ft.ftchinese.tracking.AdPosition
import com.ft.ftchinese.tracking.JSCodes
import com.ft.ftchinese.ui.web.JsSnippets

private const val TAG = "StoryBuilder"

class TemplateBuilder(private val template: String) {
    private val ctx: MutableMap<String, String> = HashMap()
    private var language: Language = Language.CHINESE
    private var shouldHideAd = false

    init {
        ctx["{{googletagservices-js}}"] = JSCodes.googletagservices
    }

    fun setLanguage(lang: Language): TemplateBuilder {
        this.language = lang
        return this
    }

    fun withFollows(follows: Map<String, String>): TemplateBuilder {

        Log.i(TAG, "$follows")
        follows.forEach { (key, value) ->
            ctx[key] = value
        }

        return this
    }

    fun withUserInfo(account: Account?): TemplateBuilder {
        if (account == null) {
            return this
        }
        ctx["<!-- AndroidUserInfo -->"] = """
<script>
var androidUserInfo = ${account.toJsonString()};
</script>""".trimIndent()

        return this
    }

    fun withAddress(addr: Address): TemplateBuilder {
        ctx["<!-- AndroidUserAddress -->"] = """
<script>
var androidUserAddress = ${addr.toJsonString()}
</script>""".trimIndent()

        return this
    }

    fun withStory(story: Story): TemplateBuilder {
        val (shouldHideAd, sponsorTitle) = story.shouldHideAd()

        var body = ""
        var title = ""
        var lang = ""
        when (this.language) {
            Language.CHINESE -> {
                body = story.getCnBody(withAd = !shouldHideAd)
                title = story.titleCN
                lang = "cn"
            }
            Language.ENGLISH -> {
                body = story.getEnBody(withAd = !shouldHideAd)
                title = story.titleEN
                lang = "en"
            }
            Language.BILINGUAL -> {
                body = story.getBilingualBody()
                title = "${story.titleCN}<br>${story.titleEN}"
                lang = "ce"
            }
        }

        // MARK: - Show a prefix for special events such as the 8.31 promotion. For now, only do this for tags because Ad sales might have different requirements.
        var storyPrefix = ""
        if (story.tag.contains(Regex("高端限免|17周年大视野精选|高端限免|17周年大視野精選"))) {
            storyPrefix = "【高端限免】"
        } else if (story.tag.contains(Regex("限免"))) {
            storyPrefix = "【限免】"
        }
        title = storyPrefix + title

        // MARK: - Show big headline for paid content
        var storyHeadlineClass = ""
        if (story.tag.contains(Regex("专享|限免|精选|双语阅读|專享|限免|精選|雙語閱讀")) || story.whitelist == 1) {
            storyHeadlineClass = " story-headline-large"
        }

        // MARK: - Get story headline styles and headshots
        val storyHeadlineStyle = getStoryHeaderStyle(story, body)
        val fullGridClass = if (body.contains("full-grid")) " full-grid-story" else ""
        val scrollyTellingClass = if (body.contains("scrollable-block")) " has-scrolly-telling" else ""
        val storyHeaderStyleClass = storyHeadlineStyle.style
        val storyBodyClass = fullGridClass + scrollyTellingClass + storyHeaderStyleClass
        val storyImageNoContainer = "<figure data-url=\"${story.cover.smallbutton}\" class=\"loading\"></figure>"
        ctx["<!--{story-body-class}-->"] = storyBodyClass
        ctx["<!--{story-image-no-container}-->"] = storyImageNoContainer
        ctx["<!--{Story-Header-Background}-->"] = storyHeadlineStyle.background
        ctx["<!--{story-author-headcut}-->"] = storyHeadlineStyle.headshot
            

        // todo
        ctx["{{story-css}}"] = ""
        ctx["{story-tag}"] = story.tag
        ctx["{story-author}"] = story.authorCN ?: ""
        ctx["{story-genre}"] = story.genre
        ctx["{story-area}"] = story.area
        ctx["{story-industry}"] = story.industry
        ctx["{story-main-topic}"] = ""
        ctx["{story-sub-topic}"] = ""
        ctx["{comments-id}"] = story.teaser?.getCommentsId() ?: ""
        // todo
        ctx["{{story-js-key}}"] = ""
        ctx["{{ad-pollyfill-js}}"] = ""
        ctx["{{db-zone-helper-js}}"] = ""



        val adTopic = story.getAdTopic()
        val cntopicScript = if (adTopic.isBlank()) "" else "window.cntopic = '$adTopic'"

        ctx["<!--{{cntopic}}-->"] = cntopicScript

        ctx["{story-language-class}"] = lang

        ctx["{Top-Banner}"] = """
            <div class="o-ads" data-o-ads-name="banner1"
            data-o-ads-center="true"
            data-o-ads-formats-default="false"
            data-o-ads-formats-small="FtcMobileBanner"
            data-o-ads-formats-medium="false"
            data-o-ads-formats-large="FtcLeaderboard"
            data-o-ads-formats-extra="FtcLeaderboard"
            data-o-ads-targeting="cnpos=top1;">
            </div>
        """.trimIndent()

        ctx["{Bottom-Banner}"] = """
            <div class="o-ads" data-o-ads-name="banner2"
            data-o-ads-center="true"
            data-o-ads-formats-default="false"
            data-o-ads-formats-small="FtcMobileBanner"
            data-o-ads-formats-medium="false"
            data-o-ads-formats-large="FtcBanner"
            data-o-ads-formats-extra="FtcBanner"
            data-o-ads-targeting="cnpos=top2;">
            </div>
        """.trimIndent()

        // Follow button
        ctx["{story-theme}"] = story.htmlForTheme(sponsorTitle)
        ctx["<!--{story-headline-class}-->"] = storyHeadlineClass
        // headline. Shown two times: one in title tag
        // other in body.
        ctx["{story-headline}"] = title
        // Lead-in
        ctx["{story-lead}"] = story.standfirstCN
        // Cover image
        ctx["{story-image}"] = story.htmlForCoverImage()
        ctx["{story-time}"] = story.formatPublishTime()
        ctx["{story-byline}"] = story.byline
        ctx["{story-body}"] = body

        ctx["{comments-order}"] = story.teaser?.getCommentsOrder() ?: ""

        // side-container
        ctx["{Right-1}"] = ""
        ctx["{story-container-style}"] = ""
        ctx["{related-stories}"] = story.htmlForRelatedStories()
        ctx["{related-topics}"] = story.htmlForRelatedTopics()

        // {ad-zone} Google广告输入正确的zone，输入值。
        // zone代表广告内容定向，发送给Google。
        // 数据来源：html div数组。

        ctx["{ad-zone}"] = story.getAdZone(Teaser.HOME_AD_ZONE, Teaser.DEFAULT_STORY_AD_ZONE, story.teaser?.channelMeta?.adZone ?: "")

        ctx["{ad-mpu}"] = if (shouldHideAd) "" else AdParser.getAdCode(AdPosition.MIDDLE_ONE)

        ctx["{adchID}"] = story.pickAdchID(Teaser.HOME_AD_CH_ID, Teaser.DEFAULT_STORY_AD_CH_ID)

        this.shouldHideAd = shouldHideAd

        return this
    }

    fun withTheme(isLight: Boolean): TemplateBuilder {
        ctx["{night-class}"] = if (isLight) "" else "night"
        return this
    }

    fun render(): String {
        var result = template

        ctx.forEach { (key, value) ->
            result = result.replace(key, value)
        }

        return JSCodes.getCleanHTML(
            JSCodes.getInlineVideo(
                AdParser.updateAdCode(result, this.shouldHideAd)
            )
        )
    }

    fun withChannel(content: String): TemplateBuilder {
        ctx["{list-content}"] = content
        return this
    }

    fun withSearch(keyword: String): TemplateBuilder {
        ctx["{search-html}"] = ""
        ctx["/*run-android-search-js*/"] = JsSnippets.search(keyword)
        return this
    }

    private data class StoryHeaderStyle(val style: String, val headshot: String, val background: String)
    private fun getStoryHeaderStyle(story: Story, body: String): StoryHeaderStyle {
        val tag = story.tag
        val imageHTML = story.htmlForCoverImage()
        val heroClass = " show-story-hero-container"
        val columnistClass = " show-story-columnist-topper"
        val pinkBackgroundClass = " story-hero-theme-pink"
        if (imageHTML != "") {
            if (body.contains("full-grid") || body.contains("scrollable-block")) {
                return StoryHeaderStyle(heroClass, "", "")
            } else if (tag.contains(Regex("FT大视野|卧底经济学家|FT杂志|FT大視野|臥底經濟學家|FT雜誌"))) {
                return StoryHeaderStyle(heroClass, "", "")
            } else if (tag.contains(Regex("周末随笔|周末隨筆"))) {
                return StoryHeaderStyle(heroClass, "", pinkBackgroundClass)
            }
        }
        return StoryHeaderStyle("", "", "")
    }
}
