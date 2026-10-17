package com.litespace.browser.ui.screens

import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.litespace.browser.R
import com.litespace.browser.core.webview.ContentBlockerEngine
import com.litespace.browser.core.webview.LiteSpaceWebViewController
import com.litespace.browser.core.webview.ProfileIsolationManager
import com.litespace.browser.data.repository.LiteAppRepository
import com.litespace.browser.domain.model.DisplayMode
import com.litespace.browser.domain.model.GlobalAppSettings
import com.litespace.browser.domain.model.LiteAppConfig
import com.litespace.browser.ui.theme.LiteSpacePullToRefreshBox
import com.litespace.browser.ui.theme.QuickControlGridTile
import com.litespace.browser.ui.theme.parseHexColorSafely
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiteAppBrowserScreen(
    initialConfig: LiteAppConfig,
    overrideUrl: String?,
    isIncognito: Boolean,
    globalSettings: GlobalAppSettings,
    repository: LiteAppRepository,
    contentBlockerEngine: ContentBlockerEngine,
    profileIsolationManager: ProfileIsolationManager,
    onShowFilePicker: (ValueCallback<Array<Uri>>, Array<String>) -> Boolean,
    onRequestPermissions: (Array<String>, (Boolean) -> Unit) -> Unit,
    onToggleImmersiveSystemBars: (Boolean) -> Unit,
    onCloseLiteApp: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var activeConfig by remember { mutableStateOf(initialConfig) }
    var currentUrl by remember { mutableStateOf(overrideUrl?.takeIf { it.isNotBlank() } ?: initialConfig.startUrl) }
    var currentTitle by remember { mutableStateOf(initialConfig.name) }
    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableIntStateOf(10) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var sessionBlockedCount by remember { mutableIntStateOf(0) }

    var isFramelessOrImmersive by remember {
        mutableStateOf(
            initialConfig.displayMode == DisplayMode.FRAMELESS ||
                initialConfig.displayMode == DisplayMode.FULLSCREEN_IMMERSIVE
        )
    }

    var showControlCenterSheet by remember { mutableStateOf(false) }
    var showFindInPageBar by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var showZoomSheet by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showLiveScriptDialog by remember { mutableStateOf(false) }

    var fullscreenVideoView by remember { mutableStateOf<View?>(null) }
    var fullscreenVideoCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val bookmarks by repository.observeBookmarks(activeConfig.id).collectAsState(initial = emptyList())
    val isCurrentPageBookmarked = bookmarks.any { it.url == currentUrl }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val webViewController = remember {
        LiteSpaceWebViewController(
            context = context,
            contentBlockerEngine = contentBlockerEngine,
            profileIsolationManager = profileIsolationManager,
            onPageStateChanged = { url, title, loading, progress, back, forward ->
                if (url.isNotBlank()) currentUrl = url
                if (title.isNotBlank()) currentTitle = title
                isLoading = loading
                loadProgress = progress
                canGoBack = back
                canGoForward = forward
            },
            onBlockedRequestHit = { matchedPattern, _ ->
                sessionBlockedCount += 1
                scope.launch {
                    repository.recordFilterRuleHit(matchedPattern)
                }
            },
            onShowFilePicker = onShowFilePicker,
            onEnterFullscreenVideo = { view, callback ->
                fullscreenVideoView = view
                fullscreenVideoCallback = callback
                onToggleImmersiveSystemBars(true)
            },
            onExitFullscreenVideo = {
                fullscreenVideoView = null
                fullscreenVideoCallback?.onCustomViewHidden()
                fullscreenVideoCallback = null
                onToggleImmersiveSystemBars(activeConfig.displayMode == DisplayMode.FULLSCREEN_IMMERSIVE)
            },
            onRequestAndroidPermissions = onRequestPermissions,
            onPopupUrlBlockedOrOpened = { _, _ -> }
        )
    }

    LaunchedEffect(activeConfig.profileId, isIncognito) {
        onToggleImmersiveSystemBars(activeConfig.displayMode == DisplayMode.FULLSCREEN_IMMERSIVE)
        profileIsolationManager.switchToProfile(
            targetProfileId = activeConfig.profileId,
            targetUrl = currentUrl,
            isIncognito = isIncognito
        )
        val restoreScript = profileIsolationManager.buildLocalStorageRestoreScript(
            profileId = activeConfig.profileId,
            currentUrl = currentUrl
        )
        if (restoreScript != null) {
            webViewRef?.evaluateJavascript(restoreScript, null)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    webViewRef?.onResume()
                    webViewRef?.resumeTimers()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    webViewRef?.onPause()
                    runCatching { CookieManager.getInstance().flush() }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val wv = webViewRef
            val finalUrl = wv?.url ?: currentUrl
            if (!isIncognito && !activeConfig.clearCookiesOnExit && !globalSettings.globalClearOnExit) {
                profileIsolationManager.captureWebViewProfileStateAsync(
                    webView = null,
                    profileId = activeConfig.profileId,
                    currentUrl = finalUrl,
                    isIncognito = false
                )
            }
            profileIsolationManager.clearSessionData(
                webView = wv,
                clearCache = activeConfig.clearCacheOnExit || globalSettings.globalClearOnExit,
                clearCookies = activeConfig.clearCookiesOnExit || globalSettings.globalClearOnExit,
                clearStorage = activeConfig.clearStorageOnExit || globalSettings.globalClearOnExit,
                isIncognito = isIncognito
            )
            runCatching {
                (wv?.parent as? ViewGroup)?.removeView(wv)
                wv?.stopLoading()
                wv?.onPause()
                wv?.removeAllViews()
                wv?.destroy()
            }
            webViewRef = null
        }
    }

    BackHandler(enabled = true) {
        when {
            fullscreenVideoView != null -> {
                fullscreenVideoCallback?.onCustomViewHidden()
                fullscreenVideoView = null
                fullscreenVideoCallback = null
            }
            showFindInPageBar -> {
                showFindInPageBar = false
                webViewRef?.clearMatches()
            }
            webViewRef?.canGoBack() == true -> {
                webViewRef?.goBack()
            }
            else -> {
                onCloseLiteApp()
            }
        }
    }

    val topBarColor = if (isIncognito) {
        Color(0xFF1E293B)
    } else {
        parseHexColorSafely(activeConfig.themeColorHex)
    }

    Scaffold(
        topBar = {
            Column {
                AnimatedVisibility(visible = !isFramelessOrImmersive) {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = topBarColor,
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White,
                            actionIconContentColor = Color.White
                        ),
                        navigationIcon = {
                            IconButton(onClick = onCloseLiteApp) {
                                Icon(Icons.Default.Close, contentDescription = "Close Lite App")
                            }
                        },
                        title = {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (currentUrl.startsWith("https://")) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = "HTTPS",
                                            modifier = Modifier.size(14.dp),
                                            tint = Color(0xFF69F0AE)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = if (isIncognito) "Incognito • $currentTitle" else currentTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${currentUrl.removePrefix("https://").removePrefix("http://")} • ${activeConfig.profileId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.82f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        actions = {
                            Surface(
                                color = Color.White.copy(alpha = 0.18f),
                                shape = CircleShape,
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .clip(CircleShape)
                                    .clickable { showControlCenterSheet = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Security,
                                        contentDescription = "Blocked count",
                                        tint = Color(0xFF69F0AE),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "$sessionBlockedCount",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            IconButton(onClick = {
                                webViewRef?.let { webViewController.toggleReaderMode(it) }
                            }) {
                                Icon(Icons.Default.MenuBook, contentDescription = stringResource(R.string.browser_reader_mode))
                            }
                            IconButton(onClick = { showControlCenterSheet = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Control Center")
                            }
                        }
                    )
                }

                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { (loadProgress / 100f).coerceIn(0.05f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                AnimatedVisibility(visible = showFindInPageBar) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = findQuery,
                                onValueChange = {
                                    findQuery = it
                                    webViewRef?.findAllAsync(it)
                                },
                                placeholder = { Text(stringResource(R.string.browser_find_in_page)) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { webViewRef?.findNext(false) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                            }
                            IconButton(onClick = { webViewRef?.findNext(true) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                            }
                            IconButton(onClick = {
                                showFindInPageBar = false
                                findQuery = ""
                                webViewRef?.clearMatches()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(visible = activeConfig.showBottomBar && !isFramelessOrImmersive) {
                BottomAppBar(
                    modifier = Modifier.height(56.dp),
                    actions = {
                        IconButton(
                            onClick = { webViewRef?.goBack() },
                            enabled = canGoBack
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.browser_back))
                        }
                        IconButton(
                            onClick = { webViewRef?.goForward() },
                            enabled = canGoForward
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.browser_forward))
                        }
                        IconButton(onClick = { webViewRef?.reload() }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.browser_reload))
                        }
                        IconButton(onClick = {
                            scope.launch {
                                if (isCurrentPageBookmarked) {
                                    bookmarks.firstOrNull { it.url == currentUrl }?.let {
                                        repository.deleteBookmark(it.id)
                                    }
                                } else {
                                    repository.addBookmark(activeConfig.id, currentTitle, currentUrl)
                                }
                            }
                        }) {
                            Icon(
                                imageVector = if (isCurrentPageBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark"
                            )
                        }
                        IconButton(onClick = { showZoomSheet = true }) {
                            Icon(Icons.Default.TextFields, contentDescription = "Font & Zoom")
                        }
                        IconButton(onClick = { showLiveScriptDialog = true }) {
                            Icon(Icons.Default.Code, contentDescription = "UserScript")
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(activeConfig.navigationGesturesEnabled) {
                    if (!activeConfig.navigationGesturesEnabled) return@pointerInput
                    var accumulatedDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { accumulatedDrag = 0f },
                        onHorizontalDrag = { _, dragAmount -> accumulatedDrag += dragAmount },
                        onDragEnd = {
                            if (accumulatedDrag > 220f && webViewRef?.canGoBack() == true) {
                                webViewRef?.goBack()
                            } else if (accumulatedDrag < -220f && webViewRef?.canGoForward() == true) {
                                webViewRef?.goForward()
                            }
                        }
                    )
                }
        ) {
            LiteSpacePullToRefreshBox(
                enabled = activeConfig.pullToRefreshEnabled,
                isRefreshing = isLoading && loadProgress < 35,
                onRefresh = { webViewRef?.reload() },
                modifier = Modifier.fillMaxSize()
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            webViewController.applyConfigToWebView(
                                webView = this,
                                config = activeConfig,
                                isIncognito = isIncognito,
                                globalAdBlock = globalSettings.globalAdBlockEnabled,
                                globalHttpsOnly = globalSettings.globalHttpsOnly
                            )
                            loadUrl(currentUrl)
                            webViewRef = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isFramelessOrImmersive) {
                SmallFloatingActionButton(
                    onClick = {
                        isFramelessOrImmersive = false
                        onToggleImmersiveSystemBars(false)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(Icons.Default.FullscreenExit, contentDescription = "Exit Fullscreen")
                }
            }

            val videoView = fullscreenVideoView
            if (videoView != null) {
                AndroidView(
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            setBackgroundColor(android.graphics.Color.BLACK)
                            (videoView.parent as? ViewGroup)?.removeView(videoView)
                            addView(
                                videoView,
                                FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }
        }
    }

    if (showControlCenterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showControlCenterSheet = false },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${activeConfig.name} • Control Center",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    QuickControlGridTile(
                        icon = Icons.Default.DesktopWindows,
                        title = stringResource(R.string.browser_desktop_mode),
                        subtitle = if (activeConfig.desktopMode) "Active" else "Mobile View",
                        isActive = activeConfig.desktopMode,
                        onClick = {
                            showControlCenterSheet = false
                            val updated = activeConfig.copy(desktopMode = !activeConfig.desktopMode)
                            activeConfig = updated
                            webViewRef?.let { wv ->
                                webViewController.applyConfigToWebView(
                                    wv,
                                    updated,
                                    isIncognito,
                                    globalSettings.globalAdBlockEnabled,
                                    globalSettings.globalHttpsOnly
                                )
                                wv.reload()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    QuickControlGridTile(
                        icon = Icons.Default.DarkMode,
                        title = stringResource(R.string.browser_dark_mode),
                        subtitle = if (activeConfig.forceDarkWeb) "On" else "Off",
                        isActive = activeConfig.forceDarkWeb,
                        onClick = {
                            showControlCenterSheet = false
                            val updated = activeConfig.copy(forceDarkWeb = !activeConfig.forceDarkWeb)
                            activeConfig = updated
                            webViewRef?.let { wv ->
                                webViewController.applyConfigToWebView(
                                    wv,
                                    updated,
                                    isIncognito,
                                    globalSettings.globalAdBlockEnabled,
                                    globalSettings.globalHttpsOnly
                                )
                                wv.reload()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    QuickControlGridTile(
                        icon = Icons.Default.MenuBook,
                        title = stringResource(R.string.browser_reader_mode),
                        subtitle = "Article View",
                        onClick = {
                            showControlCenterSheet = false
                            webViewRef?.let { webViewController.toggleReaderMode(it) }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    QuickControlGridTile(
                        icon = Icons.Default.FindInPage,
                        title = stringResource(R.string.browser_find_in_page),
                        subtitle = "Search text",
                        onClick = {
                            showControlCenterSheet = false
                            showFindInPageBar = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    QuickControlGridTile(
                        icon = Icons.Default.TextFields,
                        title = "Font & Zoom",
                        subtitle = "${activeConfig.textZoomPercent}%",
                        onClick = {
                            showControlCenterSheet = false
                            showZoomSheet = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                    QuickControlGridTile(
                        icon = Icons.Default.Code,
                        title = "CSS / JS Injector",
                        subtitle = "UserScripts",
                        isActive = activeConfig.customCss.isNotBlank() || activeConfig.customJs.isNotBlank(),
                        onClick = {
                            showControlCenterSheet = false
                            showLiveScriptDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    QuickControlGridTile(
                        icon = Icons.Default.Bookmark,
                        title = "Bookmarks",
                        subtitle = "${bookmarks.size} saved",
                        onClick = {
                            showControlCenterSheet = false
                            showBookmarksSheet = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                    QuickControlGridTile(
                        icon = Icons.Default.Fullscreen,
                        title = "Immersive Mode",
                        subtitle = "Hide bars",
                        isActive = isFramelessOrImmersive,
                        onClick = {
                            showControlCenterSheet = false
                            isFramelessOrImmersive = true
                            onToggleImmersiveSystemBars(true)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    QuickControlGridTile(
                        icon = Icons.Default.Share,
                        title = stringResource(R.string.browser_share_url),
                        onClick = {
                            showControlCenterSheet = false
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, currentUrl)
                                putExtra(Intent.EXTRA_SUBJECT, currentTitle)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, currentTitle))
                        },
                        modifier = Modifier.weight(1f)
                    )
                    QuickControlGridTile(
                        icon = Icons.Default.OpenInBrowser,
                        title = stringResource(R.string.browser_open_external),
                        onClick = {
                            showControlCenterSheet = false
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                QuickControlGridTile(
                    icon = Icons.Default.DeleteSweep,
                    title = stringResource(R.string.browser_clear_session),
                    subtitle = "Clear cookies, DOM storage & cache for current session",
                    onClick = {
                        showControlCenterSheet = false
                        profileIsolationManager.clearSessionData(
                            webView = webViewRef,
                            clearCache = true,
                            clearCookies = true,
                            clearStorage = true,
                            isIncognito = true
                        )
                        webViewRef?.reload()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(18.dp))
            }
        }
    }

    if (showZoomSheet) {
        var fontSlider by remember { mutableFloatStateOf(activeConfig.textZoomPercent.toFloat()) }
        ModalBottomSheet(onDismissRequest = { showZoomSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Text Size Scaling: ${fontSlider.toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = fontSlider,
                    onValueChange = {
                        fontSlider = it
                        webViewRef?.settings?.textZoom = it.toInt()
                    },
                    onValueChangeFinished = {
                        val updated = activeConfig.copy(textZoomPercent = fontSlider.toInt())
                        activeConfig = updated
                        scope.launch { repository.saveLiteApp(updated) }
                    },
                    valueRange = 60f..200f
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = { webViewRef?.zoomOut() }) { Text("Zoom Out (-)") }
                    TextButton(onClick = {
                        fontSlider = 100f
                        webViewRef?.settings?.textZoom = 100
                    }) { Text("Reset 100%") }
                    TextButton(onClick = { webViewRef?.zoomIn() }) { Text("Zoom In (+)") }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showBookmarksSheet) {
        ModalBottomSheet(onDismissRequest = { showBookmarksSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Saved Bookmarks (${activeConfig.name})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (bookmarks.isEmpty()) {
                    Text("No bookmarks saved for this Lite App yet.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(bookmarks, key = { it.id }) { bm ->
                            Surface(
                                onClick = {
                                    showBookmarksSheet = false
                                    webViewRef?.loadUrl(bm.url)
                                },
                                shape = MaterialTheme.shapes.medium,
                                tonalElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(bm.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Text(
                                        bm.url,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showLiveScriptDialog) {
        var cssDraft by remember { mutableStateOf(activeConfig.customCss) }
        var jsDraft by remember { mutableStateOf(activeConfig.customJs) }
        AlertDialog(
            onDismissRequest = { showLiveScriptDialog = false },
            title = { Text("Live CSS & JS UserScript Injector") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = cssDraft,
                        onValueChange = { cssDraft = it },
                        label = { Text("Custom CSS") },
                        minLines = 3,
                        maxLines = 5,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = jsDraft,
                        onValueChange = { jsDraft = it },
                        label = { Text("Custom JavaScript") },
                        minLines = 3,
                        maxLines = 5,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updated = activeConfig.copy(customCss = cssDraft, customJs = jsDraft)
                        activeConfig = updated
                        scope.launch { repository.saveLiteApp(updated) }
                        webViewRef?.let { wv ->
                            if (cssDraft.isNotBlank()) {
                                webViewController.injectRawCss(wv, cssDraft, "litespace-user-css")
                            }
                            if (jsDraft.isNotBlank()) {
                                webViewController.injectRawJavaScript(wv, jsDraft)
                            }
                        }
                        showLiveScriptDialog = false
                    }
                ) {
                    Text("Apply & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLiveScriptDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
