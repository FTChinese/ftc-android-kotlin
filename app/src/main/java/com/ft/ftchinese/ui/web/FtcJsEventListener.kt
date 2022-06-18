package com.ft.ftchinese.ui.web

import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.model.content.Following
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.store.FollowedTopics
import com.ft.ftchinese.ui.article.ArticleActivity
import com.ft.ftchinese.ui.article.ChannelActivity
import com.ft.ftchinese.ui.util.toast
import com.google.firebase.messaging.FirebaseMessaging

private const val TAG = "JsInterface"

/**
 * When you need to handle ui in the sub-class, switch
 * to UI thread as JS might be running on a non-UI thread.
 */
open class FtcJsEventListener(
    private val context: Context,
    private val channelSource: ChannelSource? = null
) : JsEventListener {
    private val topicStore = FollowedTopics.getInstance(context)

    override fun onClosePage() {

    }

    override fun onProgress(loading: Boolean) {

    }

    override fun onAlert(message: String) {
        context.toast(message)
    }

    override fun onTeasers(teasers: List<Teaser>) {

    }

    // Pass on permission in case parent channel page
    // has access control.
    @MainThread
    override fun onClickTeaser(teaser: Teaser) {
        Log.i(TAG, "onClickTeaser: $teaser")
        ArticleActivity.start(
            context,
            teaser.withParentPerm(channelSource?.permission)
        )
    }

    // Pass on parent's permission when a new channel page
    // is started from an existing channel page.
    @MainThread
    override fun onClickChannel(source: ChannelSource) {
        Log.i(TAG, "onClickChannel: $source")
        ChannelActivity.start(
            context,
            source.withParentPerm(channelSource?.permission)
        )
    }

    override fun onFollowTopic(following: Following) {
        Log.i(TAG, "onFollowTopic: $following")
        val isSubscribed = topicStore.save(following)

        if (isSubscribed) {
            FirebaseMessaging.getInstance()
                .subscribeToTopic(following.topic)
                .addOnCompleteListener { task ->
                    Log.i(
                        TAG,
                        "Subscribing to topic ${following.topic} success: ${task.isSuccessful}"
                    )
                }
        } else {
            FirebaseMessaging.getInstance()
                .unsubscribeFromTopic(following.topic)
                .addOnCompleteListener { task ->
                    Log.i(
                        TAG,
                        "Unsubscribing from topic ${following.topic} success: ${task.isSuccessful}"
                    )
                }
        }
    }
}

@Composable
fun rememberFtcJsEventListener(
    context: Context = LocalContext.current,
    channelSource: ChannelSource? = null
) = remember(channelSource) {
    FtcJsEventListener(
        context = context,
        channelSource = channelSource
    )
}
