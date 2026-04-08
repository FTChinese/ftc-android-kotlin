package com.ft.ftchinese.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.model.content.HTML_TYPE_FRAGMENT
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.model.content.WebpageMeta
import com.ft.ftchinese.model.enums.ArticleType
import com.ft.ftchinese.ui.article.ArticleActivity
import com.ft.ftchinese.ui.article.ChannelActivity
import com.ft.ftchinese.ui.main.MainActivity
import com.ft.ftchinese.ui.main.SplashActivity
import com.ft.ftchinese.ui.web.WvUrlEvent
import com.ft.ftchinese.ui.webpage.WebpageActivity

private const val TAG = "PushNotificationRouter"
private const val LOG_PREFIX = "[FTCPush]"

data class PushPayload(
    val action: String,
    val id: String,
    val title: String = "",
    val subtype: String = "",
    val audio: String = "",
)

sealed class PushRoute(
    val action: String,
    val targetId: String,
) {
    class Article(
        val teaser: Teaser,
        action: String,
    ) : PushRoute(action, teaser.id)

    class Channel(
        val source: ChannelSource,
        action: String,
        targetId: String,
    ) : PushRoute(action, targetId)

    class Web(
        val meta: WebpageMeta,
        action: String,
        targetId: String,
    ) : PushRoute(action, targetId)
}

object PushNotificationRouter {
    const val EXTRA_CONTENT_TYPE = "content_type"
    const val EXTRA_CONTENT_ID = "content_id"
    const val EXTRA_ACTION = "action"
    const val EXTRA_ID = "id"
    const val EXTRA_SUBTYPE = "subtype"
    const val EXTRA_AUDIO = "audio"
    const val EXTRA_TITLE = "title"
    const val EXTRA_LAUNCH_MODE = "push_launch_mode"
    const val LAUNCH_MODE_DIRECT = "direct"

    private const val UUID_PATTERN =
        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
    private val interactivePathIdRegex = Regex("^.*/interactive/($UUID_PATTERN|[0-9]+).*$")
    private val rawUuidRegex = Regex("^$UUID_PATTERN$")

    fun routeFromIntent(intent: Intent?): PushRoute? {
        return runCatching {
            routeFromLookup { key ->
                intent?.getStringExtra(key)
            }
        }.getOrElse { error ->
            Log.e(TAG, "$LOG_PREFIX route_from_intent_failed message=${error.message}", error)
            null
        }
    }

    fun routeFromData(
        data: Map<String, String>?,
        fallbackTitle: String? = null,
    ): PushRoute? {
        return runCatching {
            routeFromLookup { key ->
                when (key) {
                    EXTRA_TITLE -> fallbackTitle
                    else -> data?.get(key)
                }
            }
        }.getOrElse { error ->
            Log.e(TAG, "$LOG_PREFIX route_from_data_failed message=${error.message} data=$data", error)
            null
        }
    }

    fun createPendingIntent(
        context: Context,
        route: PushRoute,
    ): PendingIntent? {
        val payload = payloadFromRoute(route) ?: return null
        val intent = Intent(context, SplashActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_CONTENT_TYPE, payload.action)
            putExtra(EXTRA_CONTENT_ID, payload.id)
            putExtra(EXTRA_ACTION, payload.action)
            putExtra(EXTRA_ID, payload.id)
            putExtra(EXTRA_LAUNCH_MODE, LAUNCH_MODE_DIRECT)

            if (payload.subtype.isNotBlank()) {
                putExtra(EXTRA_SUBTYPE, payload.subtype)
            }
            if (payload.audio.isNotBlank()) {
                putExtra(EXTRA_AUDIO, payload.audio)
            }
            if (payload.title.isNotBlank()) {
                putExtra(EXTRA_TITLE, payload.title)
            }
        }
        val requestCode = "${route.action}:${route.targetId}".hashCode()

        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun start(
        context: Context,
        route: PushRoute,
        source: String,
        useParentStack: Boolean = true,
    ) {
        Log.i(
            TAG,
            "$LOG_PREFIX notification_route[$source] action=${route.action} targetId=${route.targetId} useParentStack=$useParentStack"
        )

        runCatching {
            when (route) {
                is PushRoute.Article -> {
                    if (route.teaser.type == ArticleType.Interactive &&
                        route.teaser.subType == Teaser.SUB_TYPE_RADIO
                    ) {
                        ArticleActivity.start(context, route.teaser)
                    } else if (useParentStack) {
                        ArticleActivity.startWithParentStack(context, route.teaser)
                    } else {
                        ArticleActivity.start(context, route.teaser)
                    }
                }

                is PushRoute.Channel -> {
                    if (useParentStack) {
                        ChannelActivity.startWithParentStack(context, route.source)
                    } else {
                        ChannelActivity.start(context, route.source)
                    }
                }

                is PushRoute.Web -> {
                    if (useParentStack) {
                        WebpageActivity.startWithParentStack(context, route.meta)
                    } else {
                        WebpageActivity.start(context, route.meta)
                    }
                }
            }
        }.getOrElse { error ->
            Log.e(
                TAG,
                "$LOG_PREFIX notification_route_failed[$source] action=${route.action} targetId=${route.targetId} message=${error.message}",
                error
            )
            MainActivity.start(context)
        }
    }

    fun copyExtras(
        target: Intent,
        data: Map<String, String>?,
        fallbackTitle: String? = null,
    ) {
        val payload = payloadFromLookup { key ->
            when (key) {
                EXTRA_TITLE -> fallbackTitle
                else -> data?.get(key)
            }
        } ?: return

        target.putExtra(EXTRA_CONTENT_TYPE, payload.action)
        target.putExtra(EXTRA_CONTENT_ID, payload.id)
        target.putExtra(EXTRA_ACTION, payload.action)
        target.putExtra(EXTRA_ID, payload.id)

        if (payload.subtype.isNotBlank()) {
            target.putExtra(EXTRA_SUBTYPE, payload.subtype)
        }
        if (payload.audio.isNotBlank()) {
            target.putExtra(EXTRA_AUDIO, payload.audio)
        }
        if (payload.title.isNotBlank()) {
            target.putExtra(EXTRA_TITLE, payload.title)
        }
    }

    private fun routeFromLookup(
        lookup: (String) -> String?,
    ): PushRoute? {
        val payload = payloadFromLookup(lookup) ?: return null
        return routeFromPayload(payload)
    }

    private fun payloadFromLookup(
        lookup: (String) -> String?,
    ): PushPayload? {
        val action = firstNonBlank(
            lookup,
            EXTRA_CONTENT_TYPE,
            "contentType",
            EXTRA_ACTION,
        )?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: return null

        val id = firstNonBlank(
            lookup,
            EXTRA_CONTENT_ID,
            "contentId",
            "pageId",
            EXTRA_ID,
        )?.trim()?.takeIf { it.isNotBlank() } ?: return null

        return PushPayload(
            action = action,
            id = id,
            title = firstNonBlank(lookup, EXTRA_TITLE, "headline").orEmpty().trim(),
            subtype = firstNonBlank(lookup, EXTRA_SUBTYPE, "subType", "sub")
                .orEmpty()
                .trim()
                .lowercase(),
            audio = firstNonBlank(lookup, EXTRA_AUDIO, "audioUrl").orEmpty().trim(),
        )
    }

    private fun routeFromPayload(payload: PushPayload): PushRoute? {
        val action = payload.action
        val subtype = normalizeSubtype(payload.subtype)
        val title = payload.title

        return when {
            action == "bilingual" || (action == "page" && subtype == "bilingual") -> {
                val interactiveId = extractInteractiveId(payload.id) ?: return null
                articleRoute(
                    action = action,
                    teaser = Teaser(
                        id = interactiveId,
                        type = ArticleType.Interactive,
                        subType = "bilingual",
                        title = title,
                        isCreatedFromUrl = true,
                    ),
                )
            }

            action == "story" -> articleRoute(
                action,
                Teaser(
                    id = payload.id,
                    type = ArticleType.Story,
                    title = title,
                    isCreatedFromUrl = true,
                ),
            )

            action == "premium" -> articleRoute(
                action,
                Teaser(
                    id = payload.id,
                    type = ArticleType.Premium,
                    title = title,
                    isCreatedFromUrl = true,
                ),
            )

            action == "video" -> articleRoute(
                action,
                Teaser(
                    id = payload.id,
                    type = ArticleType.Video,
                    title = title,
                    isCreatedFromUrl = true,
                ),
            )

            action == "photo" || action == "photonews" -> articleRoute(
                action,
                Teaser(
                    id = payload.id,
                    type = ArticleType.Gallery,
                    title = title,
                    isCreatedFromUrl = true,
                ),
            )

            action == "interactive" -> {
                val interactiveId = normalizeInteractivePayloadId(
                    rawId = payload.id,
                    subtype = subtype,
                )

                articleRoute(
                    action,
                    Teaser(
                        id = interactiveId,
                        type = ArticleType.Interactive,
                        subType = subtype.ifBlank { null },
                        title = title,
                        audioUrl = payload.audio.ifBlank { null },
                        radioUrl = payload.audio.ifBlank { null },
                        isCreatedFromUrl = true,
                    ),
                )
            }

            action == "gym" -> articleRoute(
                action,
                Teaser(
                    id = payload.id,
                    type = ArticleType.Interactive,
                    subType = when {
                        subtype.isBlank() -> Teaser.SUB_TYPE_MBAGYM
                        subtype == "speedreading" -> Teaser.SUB_TYPE_SPEED_READING
                        else -> subtype
                    },
                    title = title,
                    isCreatedFromUrl = true,
                ),
            )

            action == "radio" || action == "audio" -> articleRoute(
                action,
                Teaser(
                    id = payload.id,
                    type = ArticleType.Interactive,
                    subType = Teaser.SUB_TYPE_RADIO,
                    title = title,
                    audioUrl = payload.audio.ifBlank { null },
                    radioUrl = payload.audio.ifBlank { null },
                    isCreatedFromUrl = true,
                ),
            )

            action == "tag" -> PushRoute.Channel(
                source = ChannelSource(
                    title = payload.id,
                    name = "tag_${payload.id}",
                    path = "",
                    query = "",
                    htmlType = HTML_TYPE_FRAGMENT,
                ),
                action = action,
                targetId = payload.id,
            )

            action == "channel" -> PushRoute.Channel(
                source = ChannelSource(
                    title = payload.id,
                    name = "channel_${payload.id}",
                    path = "",
                    query = "",
                    htmlType = HTML_TYPE_FRAGMENT,
                ),
                action = action,
                targetId = payload.id,
            )

            action == "page" -> routePage(payload)

            else -> null
        }
    }

    private fun routePage(payload: PushPayload): PushRoute? {
        val uri = runCatching { Uri.parse(payload.id) }.getOrNull() ?: return null

        return when (val event = WvUrlEvent.fromUri(uri)) {
            is WvUrlEvent.Article -> articleRoute(
                action = payload.action,
                teaser = event.teaser.copy(title = payload.title),
            )

            is WvUrlEvent.Channel -> PushRoute.Channel(
                source = event.source,
                action = payload.action,
                targetId = payload.id,
            )

            else -> PushRoute.Web(
                meta = WebpageMeta(
                    title = payload.title,
                    url = payload.id,
                ),
                action = payload.action,
                targetId = payload.id,
            )
        }
    }

    private fun payloadFromRoute(route: PushRoute): PushPayload? {
        return when (route) {
            is PushRoute.Article -> payloadFromTeaser(
                action = route.action,
                teaser = route.teaser,
            )

            is PushRoute.Channel -> PushPayload(
                action = route.action,
                id = route.targetId,
                title = route.source.title,
            )

            is PushRoute.Web -> PushPayload(
                action = "page",
                id = route.meta.url,
                title = route.meta.title,
            )
        }
    }

    private fun payloadFromTeaser(
        action: String,
        teaser: Teaser,
    ): PushPayload? {
        val title = teaser.title
        val audio = teaser.audioUrl ?: teaser.radioUrl.orEmpty()

        return when (teaser.type) {
            ArticleType.Story -> PushPayload(
                action = "story",
                id = teaser.id,
                title = title,
            )

            ArticleType.Premium -> PushPayload(
                action = "premium",
                id = teaser.id,
                title = title,
            )

            ArticleType.Video -> PushPayload(
                action = "video",
                id = teaser.id,
                title = title,
            )

            ArticleType.Gallery -> PushPayload(
                action = "photo",
                id = teaser.id,
                title = title,
            )

            ArticleType.Interactive -> when (teaser.subType) {
                Teaser.SUB_TYPE_RADIO -> PushPayload(
                    action = "radio",
                    id = teaser.id,
                    title = title,
                    audio = audio,
                )

                Teaser.SUB_TYPE_MBAGYM -> PushPayload(
                    action = "gym",
                    id = teaser.id,
                    title = title,
                )

                Teaser.SUB_TYPE_SPEED_READING -> PushPayload(
                    action = "interactive",
                    id = teaser.id,
                    title = title,
                    subtype = Teaser.SUB_TYPE_SPEED_READING,
                )

                "bilingual" -> PushPayload(
                    action = "interactive",
                    id = teaser.id,
                    title = title,
                    subtype = "bilingual",
                )

                else -> PushPayload(
                    action = if (action.isNotBlank()) action else "interactive",
                    id = teaser.id,
                    title = title,
                    subtype = teaser.subType.orEmpty(),
                    audio = audio,
                )
            }

            ArticleType.Column -> null
        }
    }

    private fun articleRoute(
        action: String,
        teaser: Teaser,
    ): PushRoute.Article {
        return PushRoute.Article(
            teaser = teaser,
            action = action,
        )
    }

    private fun firstNonBlank(
        lookup: (String) -> String?,
        vararg keys: String,
    ): String? {
        return keys.firstNotNullOfOrNull { key ->
            lookup(key)?.takeIf { it.isNotBlank() }
        }
    }

    private fun normalizeSubtype(subtype: String): String {
        return when (subtype.trim().lowercase()) {
            "ftarticle" -> "bilingual"
            "audio" -> Teaser.SUB_TYPE_RADIO
            else -> subtype.trim().lowercase()
        }
    }

    private fun extractInteractiveId(rawId: String): String? {
        val trimmed = rawId.trim()
        if (trimmed.isBlank()) {
            return null
        }
        if (trimmed.all { it.isDigit() } || rawUuidRegex.matches(trimmed)) {
            return trimmed
        }
        return interactivePathIdRegex.find(trimmed)?.groupValues?.getOrNull(1)
    }

    private fun normalizeInteractivePayloadId(
        rawId: String,
        subtype: String,
    ): String {
        val trimmed = rawId.trim()
        if (trimmed.isBlank()) {
            return trimmed
        }
        if (subtype != "bilingual") {
            return trimmed
        }
        return extractInteractiveId(trimmed) ?: trimmed
    }
}
