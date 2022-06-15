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
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.tracking.PaywallTracker
import com.ft.ftchinese.ui.article.audio.AudioTeaserStore
import com.ft.ftchinese.ui.article.screenshot.ScreenshotParams
import com.ft.ftchinese.ui.article.share.ShareApp
import com.ft.ftchinese.ui.article.share.SocialShareList
import com.ft.ftchinese.ui.auth.AuthActivity
import com.ft.ftchinese.ui.base.toast
import com.ft.ftchinese.ui.components.rememberStartTime
import com.ft.ftchinese.ui.components.rememberWxApi
import com.ft.ftchinese.ui.components.sendArticleReadLen
import com.ft.ftchinese.ui.subs.MemberActivity
import com.ft.ftchinese.ui.subs.SubsActivity
import com.ft.ftchinese.ui.util.ShareUtils
import com.ft.ftchinese.ui.web.ComposeWebView
import com.ft.ftchinese.ui.web.rememberWebViewClient
import com.ft.ftchinese.viewmodel.UserViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.accompanist.web.rememberWebViewStateWithHTMLData
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ArticleActivityScreen(
    userViewModel: UserViewModel = viewModel(),
    scaffoldState: ScaffoldState,
    teaser: Teaser,
    onScreenshot: (ScreenshotParams) -> Unit,
    onAudio: () -> Unit,
    onBack: () -> Unit,
) {

    val context = LocalContext.current

    val baseUrl = remember(userViewModel.account) {
        Config.discoverServer(userViewModel.account)
    }

    val startTime = rememberStartTime()

    val wxApi = rememberWxApi()

    val articleState = rememberArticleState(
        scaffoldState = scaffoldState
    )

    val bottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = false,
    )
    val scope = rememberCoroutineScope()

    val wvState = rememberWebViewStateWithHTMLData(
        data = articleState.htmlLoaded,
        baseUrl = baseUrl
    )

    val webClient = rememberWebViewClient()

    LaunchedEffect(key1 = Unit) {
        articleState.initLoading(
            teaser = teaser,
            account = userViewModel.account,
        )
        articleState.trackClickTeaser(teaser)
    }

    LaunchedEffect(key1 = articleState.articleRead) {
        articleState.articleRead?.let {
            articleState.trackViewed(it)
        }
    }

    LaunchedEffect(key1 = articleState.screenshotUri) {
        articleState.screenshotUri?.let {
            onScreenshot(ScreenshotParams(
                imageUrl = it.toString(),
                articleId = teaser.id,
                articleType = teaser.type.toString()
            ))
        }
    }

    LaunchedEffect(key1 = articleState.access) {
        articleState.access?.let {
            PaywallTracker.fromArticle(teaser.withLangVariant(it.lang))
        }
    }

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

    DisposableEffect(key1 = Unit) {
        onDispose {
            val account = userViewModel.account ?: return@onDispose

            sendArticleReadLen(
                context = context,
                account = account,
                teaser = teaser,
                startAt = startTime
            )
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
                                teaser = teaser,
                                account = userViewModel.account
                            )
                        },
                        audioFound = articleState.audioFound,
                        onClickAudio = {
                            articleState.aiAudioTeaser?.let {
                                AudioTeaserStore.save(it)
                                onAudio()
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
                            teaser = teaser,
                            account = userViewModel.account
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {

                        ComposeWebView(
                            wvState = wvState,
                            webClient = webClient
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
