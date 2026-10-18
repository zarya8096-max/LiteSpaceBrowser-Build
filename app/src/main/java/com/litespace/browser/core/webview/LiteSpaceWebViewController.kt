package com.litespace.browser.core.webview

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.os.Message
import android.util.Base64
import android.view.View
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewFeature
import com.litespace.browser.domain.model.LiteAppConfig
import com.litespace.browser.domain.model.PermissionSetting
import com.litespace.browser.domain.model.PopupPolicy

class LiteSpaceWebViewController(
    context: Context,
    private val contentBlockerEngine: ContentBlockerEngine,
    private val profileIsolationManager: ProfileIsolationManager,
    private val onPageStateChanged: (url: String, title: String, isLoading: Boolean, progress: Int, canGoBack: Boolean, canGoForward: Boolean) -> Unit,
    private val onPageVisitedForHistory: (title: String, url: String) -> Unit = { _, _ -> },
    private val onBlockedRequestHit: (matchedPattern: String, blockedUrl: String) -> Unit,
    private val onShowFilePicker: (ValueCallback<Array<Uri>>, Array<String>) -> Boolean,
    private val onEnterFullscreenVideo: (View, WebChromeClient.CustomViewCallback) -> Unit,
    private val onExitFullscreenVideo: () -> Unit,
    private val onRequestAndroidPermissions: (Array<String>, (Boolean) -> Unit) -> Unit,
    private val onPopupUrlBlockedOrOpened: (popupUrl: String, wasAllowed: Boolean) -> Unit
) {
    private val appContext: Context = context.applicationContext

    val assetLoader: WebViewAssetLoader = WebViewAssetLoader.Builder()
        .setDomain("appassets.androidplatform.net")
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(appContext))
        .build()

    @Volatile
    var currentConfig: LiteAppConfig? = null
        private set

    @Volatile
    var isIncognitoSession: Boolean = false
        private set

    @SuppressLint("SetJavaScriptEnabled")
    fun applyConfigToWebView(
        webView: WebView,
        config: LiteAppConfig,
        isIncognito: Boolean,
        globalAdBlock: Boolean,
        globalHttpsOnly: Boolean
    ) {
        currentConfig = config
        isIncognitoSession = isIncognito

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(config.acceptCookies && !isIncognito)
        cookieManager.setAcceptThirdPartyCookies(
            webView,
            config.acceptCookies && !config.blockThirdPartyCookies && !isIncognito
        )

        webView.settings.apply {
            javaScriptEnabled = config.javaScriptEnabled
            domStorageEnabled = config.domStorageEnabled && !isIncognito
            databaseEnabled = config.domStorageEnabled && !isIncognito
            userAgentString = config.resolvedUserAgent()
            useWideViewPort = true
            loadWithOverviewMode = true
            textZoom = config.textZoomPercent.coerceIn(50, 250)
            builtInZoomControls = config.enableZoomControls
            displayZoomControls = false
            setSupportZoom(config.enableZoomControls)
            setSupportMultipleWindows(config.popupPolicy != PopupPolicy.BLOCK_ALL)
            javaScriptCanOpenWindowsAutomatically = config.popupPolicy == PopupPolicy.ALLOW_ALL
            mediaPlaybackRequiresUserGesture = false

            allowFileAccess = false
            allowContentAccess = false
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = false
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = false

            cacheMode = if (isIncognito || config.clearCacheOnExit) {
                WebSettings.LOAD_NO_CACHE
            } else {
                WebSettings.LOAD_DEFAULT
            }
            mixedContentMode = if (config.httpsUpgradeEnabled || globalHttpsOnly) {
                WebSettings.MIXED_CONTENT_NEVER_ALLOW
            } else {
                WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }
        }

        // Dual WebKit Dark Mode support: ALGORITHMIC_DARKENING (API 33+) + FORCE_DARK (API 29-32)
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, config.forceDarkWeb)
        }
        @Suppress("DEPRECATION")
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(
                webView.settings,
                if (config.forceDarkWeb) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
            )
        }

        webView.removeJavascriptInterface("LiteSpaceNotificationBridge")
        if (config.javaScriptEnabled) {
            webView.addJavascriptInterface(
                LiteSpaceNotificationBridge(
                    context = appContext,
                    liteAppId = config.id,
                    liteAppName = config.name,
                    permissionProvider = { currentConfig?.allowWebNotifications ?: PermissionSetting.DENY }
                ),
                "LiteSpaceNotificationBridge"
            )
        }

        webView.webViewClient = createWebViewClient(globalAdBlock, globalHttpsOnly)
        webView.webChromeClient = createWebChromeClient()
        setupDownloadListener(webView)
    }

    private fun createWebViewClient(
        globalAdBlock: Boolean,
        globalHttpsOnly: Boolean
    ): WebViewClient = object : WebViewClient() {

        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            val req = request ?: return null
            val urlObj = req.url ?: return null
            val urlString = urlObj.toString()

            val assetResponse = assetLoader.shouldInterceptRequest(urlObj)
            if (assetResponse != null) {
                return assetResponse
            }

            val cfg = currentConfig
            if (cfg != null && cfg.adBlockEnabled && globalAdBlock && !req.isForMainFrame) {
                val matchedRule = contentBlockerEngine.matchBlockedRequest(
                    requestUrl = urlString,
                    pageOriginHost = req.requestHeaders["Referer"]
                )
                if (matchedRule != null) {
                    onBlockedRequestHit(matchedRule, urlString)
                    return contentBlockerEngine.createEmptyBlockedResponse()
                }
            }

            return super.shouldInterceptRequest(view, request)
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            val webView = view ?: return false
            val uri = request?.url ?: return false
            val scheme = uri.scheme?.lowercase().orEmpty()

            if (scheme !in listOf("http", "https", "about", "javascript", "data", "blob")) {
                runCatching {
                    val externalIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    appContext.startActivity(externalIntent)
                }
                return true
            }

            val cfg = currentConfig
            if (scheme == "http" && (cfg?.httpsUpgradeEnabled == true || globalHttpsOnly)) {
                val upgradedUri = uri.buildUpon().scheme("https").build()
                webView.loadUrl(upgradedUri.toString())
                return true
            }

            if (cfg?.openExternalLinksInBrowser == true && request.isForMainFrame) {
                val appHost = runCatching { Uri.parse(cfg.startUrl).host?.lowercase().orEmpty() }.getOrDefault("")
                val targetHost = uri.host?.lowercase().orEmpty()
                if (appHost.isNotEmpty() && targetHost.isNotEmpty() &&
                    !targetHost.endsWith(appHost) && !appHost.endsWith(targetHost)
                ) {
                    runCatching {
                        appContext.startActivity(
                            Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                    return true
                }
            }

            return false
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            val webView = view ?: return
            val cfg = currentConfig ?: return

            // Reset to 0 (responsive viewport default) when defaultPageZoomPercent == 100
            webView.setInitialScale(if (cfg.defaultPageZoomPercent == 100) 0 else cfg.defaultPageZoomPercent)

            if (cfg.javaScriptEnabled) {
                injectAssetScript(webView, "scripts/notification_bridge.js")
                if (cfg.injectAtDocumentStart && cfg.customJs.isNotBlank()) {
                    injectRawJavaScript(webView, cfg.customJs)
                }
            }

            onPageStateChanged(
                url.orEmpty(),
                webView.title.orEmpty(),
                true,
                15,
                webView.canGoBack(),
                webView.canGoForward()
            )
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            val webView = view ?: return
            val cfg = currentConfig ?: return

            if (cfg.forceDarkWeb) {
                injectAssetCss(webView, "scripts/dark_mode.css", "litespace-dark-css")
            }

            if (cfg.customCss.isNotBlank()) {
                injectRawCss(webView, cfg.customCss, "litespace-user-css")
            }

            if (cfg.javaScriptEnabled && !cfg.injectAtDocumentStart && cfg.customJs.isNotBlank()) {
                injectRawJavaScript(webView, cfg.customJs)
            }

            if (!isIncognitoSession && !url.isNullOrBlank()) {
                if (!cfg.clearCookiesOnExit) {
                    profileIsolationManager.captureWebViewProfileStateAsync(
                        webView = webView,
                        profileId = cfg.profileId,
                        currentUrl = url,
                        isIncognito = false
                    )
                }
                onPageVisitedForHistory(webView.title.orEmpty(), url)
            }

            onPageStateChanged(
                url.orEmpty(),
                webView.title.orEmpty(),
                false,
                100,
                webView.canGoBack(),
                webView.canGoForward()
            )
        }
    }

    private fun createWebChromeClient(): WebChromeClient = object : WebChromeClient() {

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            val webView = view ?: return
            onPageStateChanged(
                webView.url.orEmpty(),
                webView.title.orEmpty(),
                newProgress < 100,
                newProgress,
                webView.canGoBack(),
                webView.canGoForward()
            )
        }

        override fun onReceivedTitle(view: WebView?, title: String?) {
            super.onReceivedTitle(view, title)
            val webView = view ?: return
            onPageStateChanged(
                webView.url.orEmpty(),
                title.orEmpty(),
                webView.progress < 100,
                webView.progress,
                webView.canGoBack(),
                webView.canGoForward()
            )
        }

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            val callback = filePathCallback ?: return false
            val acceptTypes = fileChooserParams?.acceptTypes
                ?.filter { it.isNotBlank() }
                ?.toTypedArray()
                ?: arrayOf("*/*")
            return onShowFilePicker(callback, acceptTypes)
        }

        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            if (view != null && callback != null) {
                onEnterFullscreenVideo(view, callback)
            } else {
                super.onShowCustomView(view, callback)
            }
        }

        override fun onHideCustomView() {
            onExitFullscreenVideo()
            super.onHideCustomView()
        }

        override fun onGeolocationPermissionsShowPrompt(
            origin: String?,
            callback: GeolocationPermissions.Callback?
        ) {
            val cb = callback ?: return
            val setting = currentConfig?.allowGeolocation ?: PermissionSetting.DENY
            when (setting) {
                PermissionSetting.DENY -> cb.invoke(origin, false, false)
                PermissionSetting.ALLOW, PermissionSetting.ASK -> {
                    onRequestAndroidPermissions(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    ) { granted ->
                        cb.invoke(origin, granted, false)
                    }
                }
            }
        }

        override fun onPermissionRequest(request: PermissionRequest?) {
            val req = request ?: return
            val cfg = currentConfig
            if (cfg == null) {
                req.deny()
                return
            }

            val requestedResources = req.resources ?: emptyArray()
            val grantedWebResources = mutableListOf<String>()
            val requiredAndroidPerms = mutableListOf<String>()

            if (requestedResources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                if (cfg.allowCamera != PermissionSetting.DENY) {
                    grantedWebResources.add(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                    requiredAndroidPerms.add(android.Manifest.permission.CAMERA)
                }
            }
            if (requestedResources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                if (cfg.allowMicrophone != PermissionSetting.DENY) {
                    grantedWebResources.add(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
                    requiredAndroidPerms.add(android.Manifest.permission.RECORD_AUDIO)
                }
            }
            if (requestedResources.contains(PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID)) {
                grantedWebResources.add(PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID)
            }

            if (grantedWebResources.isEmpty()) {
                req.deny()
                return
            }

            if (requiredAndroidPerms.isEmpty()) {
                req.grant(grantedWebResources.toTypedArray())
            } else {
                onRequestAndroidPermissions(requiredAndroidPerms.toTypedArray()) { granted ->
                    if (granted) {
                        req.grant(grantedWebResources.toTypedArray())
                    } else {
                        req.deny()
                    }
                }
            }
        }

        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?
        ): Boolean {
            val webView = view ?: return false
            val policy = currentConfig?.popupPolicy ?: PopupPolicy.BLOCK_ALL
            val hitResult = webView.hitTestResult
            val candidateUrl = hitResult.extra.orEmpty()

            if (policy == PopupPolicy.BLOCK_ALL || !isUserGesture) {
                if (candidateUrl.isNotBlank()) {
                    onPopupUrlBlockedOrOpened(candidateUrl, false)
                }
                return false
            }

            if (candidateUrl.isNotBlank()) {
                val currentHost = runCatching { Uri.parse(webView.url.orEmpty()).host?.lowercase().orEmpty() }.getOrDefault("")
                val targetHost = runCatching { Uri.parse(candidateUrl).host?.lowercase().orEmpty() }.getOrDefault("")
                val allowed = policy == PopupPolicy.ALLOW_ALL ||
                    (currentHost.isNotEmpty() && targetHost.endsWith(currentHost))
                if (allowed) {
                    webView.loadUrl(candidateUrl)
                }
                onPopupUrlBlockedOrOpened(candidateUrl, allowed)
            }
            return false
        }
    }

    private fun setupDownloadListener(webView: WebView) {
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            runCatching {
                val uri = Uri.parse(url)
                val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                val request = DownloadManager.Request(uri).apply {
                    setMimeType(mimeType)
                    val cookie = CookieManager.getInstance().getCookie(url)
                    if (!cookie.isNullOrBlank()) {
                        addRequestHeader("Cookie", cookie)
                    }
                    addRequestHeader("User-Agent", userAgent)
                    setDescription("Downloading via LiteSpace Browser")
                    setTitle(fileName)
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                }
                val dm = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)
                Toast.makeText(appContext, "Downloading $fileName…", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                Toast.makeText(appContext, "Download failed: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun toggleReaderMode(webView: WebView) {
        injectAssetScript(webView, "scripts/reader_mode.js")
    }

    private fun injectAssetCss(webView: WebView, assetPath: String, styleId: String) {
        runCatching {
            val cssText = appContext.assets.open(assetPath).bufferedReader().use { it.readText() }
            injectRawCss(webView, cssText, styleId)
        }
    }

    fun injectRawCss(webView: WebView, rawCss: String, styleId: String) {
        val base64Css = Base64.encodeToString(rawCss.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val js = """
            (function() {
                try {
                    var existing = document.getElementById('$styleId');
                    if (existing) existing.remove();
                    var binStr = window.atob('$base64Css');
                    var bytes = new Uint8Array(binStr.length);
                    for (var i = 0; i < binStr.length; i++) {
                        bytes[i] = binStr.charCodeAt(i);
                    }
                    var decodedCss = new TextDecoder('utf-8').decode(bytes);
                    var style = document.createElement('style');
                    style.id = '$styleId';
                    style.type = 'text/css';
                    style.textContent = decodedCss;
                    (document.head || document.documentElement).appendChild(style);
                } catch (e) {
                    console.error('LiteSpace CSS injection error:', e);
                }
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun injectAssetScript(webView: WebView, assetPath: String) {
        runCatching {
            val jsText = appContext.assets.open(assetPath).bufferedReader().use { it.readText() }
            injectRawJavaScript(webView, jsText)
        }
    }

    fun injectRawJavaScript(webView: WebView, rawJs: String) {
        val base64Js = Base64.encodeToString(rawJs.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val wrapper = """
            (function() {
                try {
                    var binStr = window.atob('$base64Js');
                    var bytes = new Uint8Array(binStr.length);
                    for (var i = 0; i < binStr.length; i++) {
                        bytes[i] = binStr.charCodeAt(i);
                    }
                    var decodedJs = new TextDecoder('utf-8').decode(bytes);
                    var scriptEl = document.createElement('script');
                    scriptEl.type = 'text/javascript';
                    scriptEl.textContent = decodedJs;
                    (document.head || document.documentElement).appendChild(scriptEl);
                } catch (e) {
                    console.error('LiteSpace UserScript error:', e);
                }
            })();
        """.trimIndent()
        webView.evaluateJavascript(wrapper, null)
    }
}
