package com.ft.ftchinese.ui.article.content

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.R
import com.ft.ftchinese.database.ReadArticle
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.tracking.PaywallTracker
import com.ft.ftchinese.ui.article.NavStore
import com.ft.ftchinese.ui.article.share.ShareApp
import com.ft.ftchinese.ui.article.share.SocialShareList
import com.ft.ftchinese.ui.auth.AuthActivity
import com.ft.ftchinese.ui.components.rememberBaseUrl
import com.ft.ftchinese.ui.components.rememberStartTime
import com.ft.ftchinese.ui.components.rememberWxApi
import com.ft.ftchinese.ui.components.sendArticleReadLen
import com.ft.ftchinese.ui.subs.MemberActivity
import com.ft.ftchinese.ui.subs.SubsActivity
import com.ft.ftchinese.ui.util.AccountAction
import com.ft.ftchinese.ui.util.IntentsUtil
import com.ft.ftchinese.ui.util.ShareUtils
import com.ft.ftchinese.ui.util.toast
import com.ft.ftchinese.ui.web.FtcJsEventListener
import com.ft.ftchinese.ui.web.FtcWebView
import com.ft.ftchinese.ui.web.WebViewCallback
import com.ft.ftchinese.viewmodel.UserViewModel
import com.google.accompanist.web.rememberWebViewStateWithHTMLData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

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

    val account by userViewModel.accountLiveData.observeAsState()
    val startTime = rememberStartTime()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val articleState = rememberArticleState(
        scaffoldState = scaffoldState,
        scope = scope
    )

    // Find current article teaser by md5 hash.
    // This must be called before loading article.
    LaunchedEffect(key1 = Unit) {
        id?.let {
            articleState.findTeaser(it)
        }
    }

    val baseUrl = rememberBaseUrl(account)
    val wxApi = rememberWxApi()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        // Handle result for login, payment, and refreshing account
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val a = userViewModel.reloadAccount()
                // DO not use the variable `account` defined at the start of this composable.
                // The state value captured here won't be updated.
                articleState.refreshAccess(a)
                result.data?.let(IntentsUtil::getAccountAction)?.let {
                    when (it) {
                        // Result from AuthActivity.
                        AccountAction.SignedIn -> {
                            context.toast(R.string.login_success)
                        }
                        // Result from MemberActivity or SubsActivity
                        AccountAction.Refreshed -> {
                            context.toast(R.string.refresh_success)
                        }
                        else -> { }
                    }
                }
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

    val screenshotWVCb = remember {
        object : WebViewCallback(context) {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                articleState.createScreenshot()
            }
        }
    }

    val (showScreenshot, setShowScreenshot) = remember {
        mutableStateOf(false)
    }

    // Start loading data.
    LaunchedEffect(
        key1 = articleState.currentTeaser,
        key2 = baseUrl
    ) {
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
            setShowScreenshot(false)
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

    // Track reading duration upon unmounting this component.
    DisposableEffect(key1 = Unit) {
        onDispose {
            val a = userViewModel.account ?: return@onDispose
            articleState.currentTeaser?.let {
                sendArticleReadLen(
                    context = context,
                    account = a,
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
                            setShowScreenshot(true)
//                            articleState.createScreenshot()
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
        SwipeBackContainer(
            enabled = !bottomSheetState.isVisible && !showScreenshot,
            onBack = onBack,
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

                FtcWebView(
                    wvState = wvState,
                    webClientCallback = wvCallback,
                    jsListener = jsCallback,
                    modifier = Modifier.fillMaxSize()
                ) {
                    articleState.onWebViewCreated(it)
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
                                    premiumFirst = event.upgrade
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

            if (showScreenshot) {
                Box(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    FtcWebView(
                        wvState = wvState,
                        webClientCallback = screenshotWVCb
                    ) {
                        articleState.onScreenshotWV(it)
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeBackContainer(
    enabled: Boolean,
    onBack: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val swipeOffset = remember { Animatable(0f) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val containerWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val dismissThresholdPx = containerWidthPx * 0.3f
        val offsetPx = if (isDragging) dragOffset else swipeOffset.value
        val shadowProgress = (offsetPx / containerWidthPx).coerceIn(0f, 1f)

        LaunchedEffect(enabled) {
            if (!enabled && offsetPx > 0f) {
                settleJob?.cancel()
                settleJob = null
                isDragging = false
                swipeOffset.snapTo(offsetPx)
                swipeOffset.animateTo(0f, animationSpec = tween(durationMillis = 120))
                dragOffset = 0f
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.08f * (1f - shadowProgress)))
        )

        val gestureModifier = if (enabled) {
            Modifier.pointerInput(containerWidthPx, enabled) {
                awaitEachGesture {
                    val horizontalDominanceRatio = 1.35f
                    var accumulatedDx = 0f
                    var accumulatedDy = 0f
                    var lockedToSwipeBack = false
                    var rejectedAsVerticalScroll = false

                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial
                    )

                    // If a settle animation is in progress, hand control back to this gesture.
                    settleJob?.cancel()
                    settleJob = null
                    isDragging = false
                    dragOffset = swipeOffset.value

                    var pointerId = down.id
                    val touchSlop = viewConfiguration.touchSlop

                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == pointerId }
                            ?: event.changes.firstOrNull()
                            ?: break

                        pointerId = change.id

                        if (!change.pressed) {
                            break
                        }

                        val delta = change.positionChange()

                        if (!lockedToSwipeBack && !rejectedAsVerticalScroll) {
                            accumulatedDx += delta.x
                            accumulatedDy += delta.y

                            val absDx = abs(accumulatedDx)
                            val absDy = abs(accumulatedDy)
                            val passedSlop = absDx > touchSlop || absDy > touchSlop

                            if (!passedSlop) {
                                continue
                            }

                            val shouldLockHorizontal =
                                accumulatedDx > 0f && absDx > absDy * horizontalDominanceRatio

                            if (!shouldLockHorizontal) {
                                rejectedAsVerticalScroll = true
                                continue
                            }

                            lockedToSwipeBack = true
                            isDragging = true
                            val overSlop = (absDx - touchSlop).coerceAtLeast(0f)
                            val nextOffset = (dragOffset + overSlop)
                                .coerceIn(0f, containerWidthPx)
                            change.consume()
                            dragOffset = nextOffset
                            continue
                        }

                        if (!lockedToSwipeBack) {
                            continue
                        }

                        val nextOffset = (dragOffset + delta.x)
                            .coerceIn(0f, containerWidthPx)

                        if (nextOffset != dragOffset) {
                            change.consume()
                            dragOffset = nextOffset
                        }
                    }

                    if (!lockedToSwipeBack) {
                        isDragging = false
                    } else {
                        val finalOffset = dragOffset
                        isDragging = false

                        settleJob?.cancel()
                        settleJob = scope.launch {
                            swipeOffset.snapTo(finalOffset)

                            if (finalOffset >= dismissThresholdPx) {
                                swipeOffset.animateTo(
                                    targetValue = containerWidthPx,
                                    animationSpec = tween(durationMillis = 140)
                                )
                                onBack()
                                swipeOffset.snapTo(0f)
                                dragOffset = 0f
                                settleJob = null
                                return@launch
                            }

                            swipeOffset.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = 180)
                            )
                            dragOffset = 0f
                            settleJob = null
                        }
                    }
                }
            }
        } else {
            Modifier
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(gestureModifier)
                .offset { IntOffset(offsetPx.roundToInt(), 0) }
                .graphicsLayer {
                    shadowElevation = if (offsetPx > 0f) {
                        with(density) { 8.dp.toPx() }
                    } else {
                        0f
                    }
                },
            content = content
        )
    }
}

private fun openInBrowser(
    context: Context,
    article: ReadArticle
) {
    try {
        val webpageUri = Uri.parse(article.canonicalUrl)
        Log.i("ArticleActivity", "$webpageUri")
        IntentsUtil.openInBrowser(context, webpageUri)
    } catch (e: Exception) {
        e.message?.let { context.toast(it) }
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
