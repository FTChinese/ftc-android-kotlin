package com.ft.ftchinese.ui.article.content

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.R
import com.ft.ftchinese.database.ReadArticle
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.tracking.PaywallTracker
import com.ft.ftchinese.ui.article.NavStore
import com.ft.ftchinese.ui.article.share.ShareApp
import com.ft.ftchinese.ui.article.share.SocialShareList
import com.ft.ftchinese.ui.auth.AuthActivity
import com.ft.ftchinese.ui.util.toast
import com.ft.ftchinese.ui.components.rememberStartTime
import com.ft.ftchinese.ui.components.rememberWxApi
import com.ft.ftchinese.ui.components.sendArticleReadLen
import com.ft.ftchinese.ui.subs.MemberActivity
import com.ft.ftchinese.ui.subs.SubsActivity
import com.ft.ftchinese.ui.util.ShareUtils
import com.ft.ftchinese.ui.web.FtcJsEventListener
import com.ft.ftchinese.ui.web.FtcWebView
import com.ft.ftchinese.ui.web.WebViewCallback
import com.ft.ftchinese.viewmodel.UserViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.accompanist.web.rememberWebViewStateWithHTMLData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ArticleActivityScreen(
    userViewModel: UserViewModel = viewModel(),
    scaffoldState: ScaffoldState,
    id: String?,
    onScreenshot: (id: String) -> Unit,
    onAudio: (id: String) -> Unit,
    onArticle: (id: String) -> Unit,
    onChannel: (id: String) -> Unit,
    onBack: () -> Unit,
) {

    val startTime = rememberStartTime()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val articleState = rememberArticleState(
        scaffoldState = scaffoldState,
        scope = scope
    )

    LaunchedEffect(key1 = Unit) {
        id?.let {
            articleState.findTeaser(it)
        }
    }

    val baseUrl = remember(userViewModel.account) {
        Config.discoverServer(userViewModel.account)
    }
    val wxApi = rememberWxApi()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                userViewModel.reloadAccount()
                articleState.refreshAccess(userViewModel.account)
            }
            Activity.RESULT_CANCELED -> {

            }
        }
    }

    val bottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = false,
    )

    val wvState = rememberWebViewStateWithHTMLData(
        data = articleState.htmlLoaded,
        baseUrl = baseUrl
    )

    val jsCallback = remember {
        object : FtcJsEventListener(context) {
            override fun onClickTeaser(teaser: Teaser) {
                scope.launch(Dispatchers.Main) {
                    onArticle(NavStore.saveTeaser(teaser))
                }
            }

            override fun onClickChannel(source: ChannelSource) {
                scope.launch(Dispatchers.Main) {
                    onChannel(NavStore.saveChannel(source))
                }
            }
        }
    }

    val wvCallback = remember(userViewModel.account) {
        object : WebViewCallback(context) {
            override fun onClickChannel(source: ChannelSource) {
                onChannel(NavStore.saveChannel(source))
            }

            override fun onClickStory(teaser: Teaser) {
                onArticle(NavStore.saveTeaser(teaser))
            }

            override fun onLogin() {
                if (userViewModel.account == null) {
                    AuthActivity.launch(
                        launcher = launcher,
                        context = context,
                    )
                } else {
                    context.toast("Already logged in")
                }
            }
        }
    }

    LaunchedEffect(key1 = articleState.currentTeaser) {
        if (articleState.currentTeaser != null) {
            articleState.initLoading(
                account = userViewModel.account,
            )
        }
    }

    LaunchedEffect(key1 = articleState.articleRead) {
        articleState.articleRead?.let {
            articleState.trackViewed(it)
        }
    }

    LaunchedEffect(key1 = articleState.screenshotMeta) {
        articleState.screenshotMeta?.let {
            onScreenshot(NavStore.saveScreenshot(it))
        }
    }

    LaunchedEffect(key1 = articleState.access) {
        articleState.access?.let { access ->
            articleState.currentTeaser?.let {
                PaywallTracker.fromArticle(it.withLangVariant(access.lang))
            }
        }
    }

    DisposableEffect(key1 = Unit) {
        onDispose {
            val account = userViewModel.account ?: return@onDispose
            articleState.currentTeaser?.let {
                sendArticleReadLen(
                    context = context,
                    account = account,
                    teaser = it,
                    startAt = startTime
                )
            }
        }
    }

    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetContent = {
            SocialShareList(
                apps = ShareApp.all,
                onShareTo = {
                    scope.launch {
                        bottomSheetState.hide()
                    }

                    val article = articleState.articleRead
                    if (article == null) {
                        context.toast("Missing share data")
                        return@SocialShareList
                    }

                    when (it) {
                        ShareApp.WxFriend,
                        ShareApp.WxMoments -> {
                            wxApi.sendReq(
                                ShareUtils.wxShareArticleReq(
                                    res = context.resources,
                                    app= it,
                                    article = article,
                                )
                            )
                            articleState.trackShare(article)
                        }
                        ShareApp.Screenshot -> {
                            articleState.createScreenshot()
                        }
                        ShareApp.Browser -> {
                            openInBrowser(
                                context,
                                article
                            )
                        }
                        ShareApp.More -> {
                            launchDefaultShare(
                                context,
                                article,
                            )
                        }
                    }
                }
            )
        },
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            ArticleLayout(
                modifier = Modifier.align(Alignment.TopStart),
                loading = articleState.progress.value,
                topBar = {
                    ArticleToolBar(
                        isBilingual = articleState.isBilingual,
                        currentLang = articleState.language,
                        onSelectLang = {
                            articleState.switchLang(
                                lang = it,
                                account = userViewModel.account
                            )
                        },
                        audioFound = articleState.audioFound,
                        onClickAudio = {
                            articleState
                                .aiAudioTeaser
                                ?.let {
                                    onAudio(NavStore.saveTeaser(it))
                                }
                        },
                        onBack = onBack,
                    )
                },
                bottomBar = {
                    ArticleBottomBar(
                        bookmarked = articleState.bookmarked,
                        onBookmark = {
                            articleState.bookmark(it)
                        },
                        onShare = {
                            scope.launch {
                                bottomSheetState.show()
                            }
                        }
                    )
                },
            ) {
                SwipeRefresh(
                    state = rememberSwipeRefreshState(
                        isRefreshing = articleState.refreshing
                    ),
                    onRefresh = {
                        articleState.refresh(
                            account = userViewModel.account
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // SwipeRefresh must have a vertical scrollable children.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {

                        FtcWebView(
                            wvState = wvState,
                            webClientCallback = wvCallback,
                            jsListener = jsCallback,
                        ) {
                            articleState.onWebViewCreated(it)
                        }
                    }
                }
            }

            articleState.access?.let {

                if (it.granted) {
                    return@let
                }

                PermissionBarrier(
                    modifier = Modifier.align(Alignment.TopStart),
                    access = it,
                    onClick = { event ->
                        when (event) {
                            is BarrierEvent.Login -> {
                                AuthActivity.launch(
                                    launcher = launcher,
                                    context = context,
                                )
                            }
                            is BarrierEvent.Paywall -> {
                                SubsActivity.launch(
                                    launcher = launcher,
                                    context = context,
                                )
                            }
                            is BarrierEvent.MySubs -> {
                                MemberActivity.launch(
                                    launcher = launcher,
                                    context = context
                                )
                            }
                            is BarrierEvent.Quit -> {
                                if (it.isSwitchLanguage) {
                                    // For language switch, revert access to chinese edition.
                                    articleState.refreshAccess(userViewModel.account)
                                } else {
                                    onBack()
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

private fun openInBrowser(
    context: Context,
    article: ReadArticle
) {
    try {
        val webpageUri = Uri.parse(article.canonicalUrl)
        val intent = Intent(Intent.ACTION_VIEW, webpageUri)
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        context.toast("URL not found")
    }
}

private fun launchDefaultShare(
    context: Context,
    article: ReadArticle,
) {
    val shareString = context.getString(
        R.string.share_template,
        article.title,
        article.canonicalUrl)

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareString)
        type = "text/plain"
    }
    context.startActivity(
        Intent.createChooser(
            sendIntent,
            context.getString(R.string.share_to),
        )
    )
}
