package com.litespace.browser.core.webview

import android.content.Context
import android.webkit.JavascriptInterface
import com.litespace.browser.domain.model.PermissionSetting
import com.litespace.browser.worker.FeedNotificationWorker

class LiteSpaceNotificationBridge(
    private val context: Context,
    private val liteAppId: Long,
    private val liteAppName: String,
    private val permissionProvider: () -> PermissionSetting
) {

    @JavascriptInterface
    fun getPermissionState(origin: String?): String {
        return when (permissionProvider()) {
            PermissionSetting.ALLOW -> "granted"
            PermissionSetting.DENY -> "denied"
            PermissionSetting.ASK -> "default"
        }
    }

    @JavascriptInterface
    fun requestPermissionFromPage(origin: String?): String {
        return when (permissionProvider()) {
            PermissionSetting.ALLOW, PermissionSetting.ASK -> "granted"
            PermissionSetting.DENY -> "denied"
        }
    }

    @JavascriptInterface
    fun postWebNotification(
        title: String?,
        body: String?,
        tag: String?,
        pageUrl: String?
    ) {
        val perm = permissionProvider()
        if (perm == PermissionSetting.DENY) return

        val safeTitle = title?.takeIf { it.isNotBlank() } ?: liteAppName
        val safeBody = body?.takeIf { it.isNotBlank() } ?: safeTitle
        val targetUrl = pageUrl?.takeIf { it.isNotBlank() } ?: ""

        FeedNotificationWorker.postFeedNotification(
            context = context.applicationContext,
            liteAppId = liteAppId,
            liteAppName = safeTitle,
            entryTitle = safeBody,
            entryLink = targetUrl
        )
    }
}
