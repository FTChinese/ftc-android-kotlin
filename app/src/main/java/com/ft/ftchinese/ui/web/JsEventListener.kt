package com.ft.ftchinese.ui.web

import android.content.Context
import android.util.Log
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.model.content.Following
import com.ft.ftchinese.model.content.FollowingManager
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.ui.article.ArticleActivity
import com.ft.ftchinese.ui.base.toast
import com.ft.ftchinese.ui.channel.ChannelActivity
import com.google.firebase.messaging.FirebaseMessaging

interface JsEventListener {
    fun onClosePage()
    fun onProgress(loading: Boolean)
    fun onAlert(message: String)
    fun onTeaserClicked(teaser: Teaser)
    fun onChannelClicked(source: ChannelSource)
    fun onFollowTopic(following: Following)
}

class DumbJsEventListener : JsEventListener {
    override fun onClosePage() {
    }

    override fun onProgress(loading: Boolean) {
    }

    override fun onAlert(message: String) {
    }

    override fun onTeaserClicked(teaser: Teaser) {
    }

    override fun onChannelClicked(source: ChannelSource) {
    }

    override fun onFollowTopic(following: Following) {
    }
}

open class BaseJsEventListener(
    private val context: Context,
    private val channelSource: ChannelSource? = null
) : JsEventListener {
    private val topicStore = FollowingManager.getInstance(context)

    override fun onClosePage() {

    }

    override fun onProgress(loading: Boolean) {

    }

    override fun onAlert(message: String) {
        context.toast(message)
    }

    override fun onTeaserClicked(teaser: Teaser) {
        ArticleActivity.start(
            context,
            teaser.withParentPerm(channelSource?.permission)
        )
    }

    override fun onChannelClicked(source: ChannelSource) {
        ChannelActivity.start(context, source)
    }

    override fun onFollowTopic(following: Following) {
        val isSubscribed = topicStore.save(following)

        if (isSubscribed) {
            FirebaseMessaging.getInstance()
                .subscribeToTopic(following.topic)
                .addOnCompleteListener { task ->
                    Log.i(ArticleActivity.TAG, "Subscribing to topic ${following.topic} success: ${task.isSuccessful}")
                }
        } else {
            FirebaseMessaging.getInstance()
                .unsubscribeFromTopic(following.topic)
                .addOnCompleteListener { task ->
                    Log.i(ArticleActivity.TAG, "Unsubscribing from topic ${following.topic} success: ${task.isSuccessful}")
                }
        }
    }
}
