package com.ft.ftchinese

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.BottomSheetDialog
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.webkit.*
import android.widget.ImageView
import android.widget.TextView
import com.ft.ftchinese.database.ArticleStore
import com.ft.ftchinese.models.ChannelItem
import com.ft.ftchinese.models.Following
import com.ft.ftchinese.models.FollowingManager
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.util.gson
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.android.synthetic.main.activity_content.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast
import java.io.ByteArrayOutputStream

/**
 * This is used to show the contents of an article in web view.
 * Subclass must implement `load` method to handle data fetching.
 * Subclass must call `onCreate`.
 */
abstract class AbsContentActivity : AppCompatActivity(),
        SwipeRefreshLayout.OnRefreshListener,
        AnkoLogger {

    private var mBottomDialog: BottomSheetDialog? = null

    // Used for share
    abstract val articleWebUrl: String
    abstract val articleTitle: String
    abstract val articleStandfirst: String
    /**
     * Do not use this value until it is initialized!
     */
    protected abstract var mChannelItem: ChannelItem?

    protected var mSessionManager: SessionManager? = null
    protected var mFollowingManager: FollowingManager? = null
    protected var mArticleStore: ArticleStore? = null

    private var _starring: Boolean = false
    private var mIsStarring: Boolean
        get() = _starring
        set(value) {
            _starring = value
            if (value) {
                action_favourite.setImageResource(R.drawable.ic_favorite_teal_24dp)
            } else {
                action_favourite.setImageResource(R.drawable.ic_favorite_border_teal_24dp)
            }
        }

    protected var isInProgress: Boolean = false
        set(value) {
            if (value) {
                progress_bar.visibility = View.VISIBLE
            } else {
                swipe_refresh.isRefreshing = value
                progress_bar.visibility = View.GONE
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_content)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            // Do not show title on the toolbar for any content.
            setDisplayShowTitleEnabled(false)
        }

        mSessionManager = SessionManager.getInstance(this)
        mFollowingManager = FollowingManager.getInstance(this)
        mArticleStore = ArticleStore.getInstance(this)

        swipe_refresh.setOnRefreshListener(this)

        // Configure WebView
        web_view.settings.apply {
            javaScriptEnabled = true
            loadsImagesAutomatically = true
        }

        web_view.apply {

            // Binding JavaScript code to Android Code
            addJavascriptInterface(ContentWebViewInterface(), "Android")

            // Set a WebViewClient to handle various links in the WebView
            webViewClient = WVClient(this@AbsContentActivity)

            // Set the chrome handler
            webChromeClient = ChromeClient()

            // Handle Back button
            setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_BACK && web_view.canGoBack()) {
                    web_view.goBack()
                    return@setOnKeyListener true
                }

                false
            }
        }

        // Handle star/unstar action
        action_favourite.setOnClickListener {
            // Save to SQL
            GlobalScope.launch {
                // If is starring currently, remove it
                if (mIsStarring) {
                    info("Unstar article: $mChannelItem")
                    val affectedRows = mArticleStore?.deleteStarred(mChannelItem) ?: return@launch

                    // Turn is starting to false after deleted.
                    if (affectedRows > 0) {
                        mIsStarring  = false
                    }

                    return@launch
                }

                // If is not starring, add it.
                info("Start article: $mChannelItem")
                val rowId = mArticleStore?.addStarred(mChannelItem) ?: return@launch

                // Turn is starring to true after added.
                if (rowId > 0) {
                    mIsStarring = true
                }
            }
        }

        action_share.setOnClickListener {
            if (mBottomDialog == null) {
                mBottomDialog = BottomSheetDialog(this)
                mBottomDialog?.setContentView(R.layout.fragment_share_menu)

                val shareRecyclerView: RecyclerView? = mBottomDialog?.findViewById(R.id.share_recycler_view)

                shareRecyclerView?.apply {
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(this@AbsContentActivity).apply {
                        orientation = LinearLayoutManager.HORIZONTAL
                    }
                    adapter = ShareAdapter()
                }
            }

            mBottomDialog?.show()
        }
    }

    protected fun updateStarUI() {
        GlobalScope.launch(Dispatchers.Main) {
            mIsStarring = async {
                mArticleStore?.isStarring(mChannelItem)
            }.await() ?: false
        }
    }

    // Create options menu
//    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        super.onCreateOptionsMenu(menu)
//        menuInflater.inflate(R.menu.activity_content_list, menu)
//
//        /**
//         * Docs on share:
//         * https://developer.android.com/training/appbar/action-views
//         * https://developer.android.com/training/sharing/shareaction
//         * https://developer.android.com/reference/android/support/v7/widget/ShareActionProvider
//         * How to set intent: https://developer.android.com/training/sharing/send
//         */
////        menu?.findItem(R.id.action_share).also { menuItem ->
////            mShareActionProvider = MenuItemCompat.getActionProvider(menuItem) as ShareActionProvider
////        }
//
//        return true
//    }

    /**
     * Called by subclass to set share intent for ShareActionProvider
     */
//    fun setShareIntent(shareIntent: Intent) {
//        mShareActionProvider?.setShareIntent(Intent.createChooser(shareIntent, "分享到"))
//    }

    // Handle menu click events
    override fun onOptionsItemSelected(item: MenuItem?) = when (item?.itemId) {
        R.id.action_listen -> {

            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    abstract fun load()

    fun showProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            swipe_refresh.isRefreshing = false
            progress_bar.visibility = View.GONE
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconView: ImageView = itemView.findViewById(R.id.share_icon_view)
        val textView: TextView = itemView.findViewById(R.id.share_text_view)
    }

    inner class ShareAdapter : RecyclerView.Adapter<ViewHolder>() {

        private val apps = arrayOf(
                ShareItem("好友", R.drawable.wechat, ShareItem.WECHAT_FRIEND),
                ShareItem("朋友圈", R.drawable.moments, ShareItem.WECHAT_MOMOMENTS),
                ShareItem("打开链接", R.drawable.chrome, ShareItem.OPEN_IN_BROWSER),
                ShareItem("更多", R.drawable.ic_more_horiz_black_24dp, ShareItem.MORE_OPTIONS)
        )

        private var api: IWXAPI = WXAPIFactory.createWXAPI(this@AbsContentActivity, BuildConfig.WECAHT_APP_ID, false)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(this@AbsContentActivity)
                    .inflate(R.layout.list_item_share, parent, false)

            return ViewHolder(view)
        }

        override fun getItemCount(): Int {
            return apps.size
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = apps[position]

            holder.iconView.setImageResource(app.icon)
            holder.textView.text = app.appName

            holder.itemView.setOnClickListener {

                when (app.id) {
                    ShareItem.WECHAT_FRIEND, ShareItem.WECHAT_MOMOMENTS -> {

                        val webpage = WXWebpageObject()
                        webpage.webpageUrl = articleWebUrl

                        val msg = WXMediaMessage(webpage)
                        msg.title = articleTitle
                        msg.description = articleStandfirst

                        val bmp = BitmapFactory.decodeResource(resources, R.drawable.brand_ftc_logo_square_48)
                        val thumbBmp = Bitmap.createScaledBitmap(bmp, 150, 150, true)
                        bmp.recycle()
                        msg.thumbData = bmpToByteArray(thumbBmp, true)

                        val req = SendMessageToWX.Req()
                        req.transaction = System.currentTimeMillis().toString()
                        req.message = msg
                        req.scene = if (app.id == ShareItem.WECHAT_FRIEND) SendMessageToWX.Req.WXSceneSession else SendMessageToWX.Req.WXSceneTimeline

                        api.sendReq(req)
                    }

                    ShareItem.OPEN_IN_BROWSER -> {
                        val webpage = Uri.parse(articleWebUrl)
                        val intent = Intent(Intent.ACTION_VIEW, webpage)
                        if (intent.resolveActivity(packageManager) != null) {
                            startActivity(intent)
                        }
                    }

                    ShareItem.MORE_OPTIONS -> {
                        val shareString = getString(R.string.share_template, articleTitle, articleWebUrl)

                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareString)
                            type = "text/plain"
                        }
                        startActivity(Intent.createChooser(sendIntent, "分享到"))
                    }
                }

                mBottomDialog?.dismiss()
            }
        }

    }

    /**
     * Methods injected to JavaScript in WebView.
     * This is used to handle click event for a story.
     */
    inner class ContentWebViewInterface : AnkoLogger {

        /**
         * Handle the 关注 button in WebView.
         * When user clicked a button, data will be passed from WebView:
         * {
         *  tag: "中国经济",
         *  type: null,
         *  action: "follow | unfollow"
         * }
         * Data is saved in or removed from
         * shared preference depending on the value of
         * action.
         *
         * When loading HTML into WebView, you have to
         * replace those values in HTML so that JS
         * could know whether the current story is
         * being followed:
         *
         * var follows = {
         *  'tag': ['{follow-tags}'],
         *  'topic': ['{follow-topics}'],
         *  'industry': ['{follow-industries}'],
         *  'area': ['{follow-areas}'],
         *  'augthor': ['{follow-authors}'],
         *  'column': ['{follow-columns}']
         *  }
         *
         * An example of what does this string look liks:
         * var follows = {
         * 'tag': ['中国经济', '香港'],
         * 'topic': ['management'],
         * 'industry': ['technology', 'media'],
         * 'area': ['china', 'us'],
         * 'augthor': ['Martin Wolf'],
         * 'column': ['10002']
         * }
         *
         * NOTE: `augthor` might be a typo in the
         * source code. Just follow it.
         *
         * You also need to replace `{story-theme}` in
         * in HTML so that the 关注 could be displayed
         * in a webview. The HTML should be:
         * <div class="story-theme">
         *     <a target="_blank" href="/tag/香港">香港</a>
         *     <button class="myft-follow tick" data-tag="香港" data-type="tag">已关注</button>
         * </div>
         *
         * This string is generated by Story#htmlForTheme in models.Article.kt
         *
         * The replacement of HTML content happens in
         * ChannelItem#render().
         *
         *  See Web-NewFTCiPhone/app/templates/story.html for the HTML codes.
         */
        @JavascriptInterface
        fun follow(message: String) {
            info("Clicked a follow button")
            info("Received follow message: $message")

            val following = gson.fromJson<Following>(message, Following::class.java)
            mFollowingManager?.save(following)
        }

        /**
         * Handle login in webview. Usage: Android.login(message: String)
         */
        @JavascriptInterface
        fun login(message: String) {
            info("Login data from webview")
            toast("WebView login: $message")
        }
    }
}

data class ShareItem(
        val appName: CharSequence,
        val icon: Int,
        val id: Int
) {
    companion object {
        const val WECHAT_FRIEND = 1
        const val WECHAT_MOMOMENTS = 2
        const val OPEN_IN_BROWSER = 3
        const val MORE_OPTIONS = 4
    }
}

fun bmpToByteArray(bmp: Bitmap, needRecycle: Boolean): ByteArray {
		val output = ByteArrayOutputStream();
		bmp.compress(Bitmap.CompressFormat.PNG, 100, output);
		if (needRecycle) {
			bmp.recycle()
		}

		val result = output.toByteArray()
		try {
			output.close();
		} catch (e: Exception) {
			e.printStackTrace()
		}

		return result
	}