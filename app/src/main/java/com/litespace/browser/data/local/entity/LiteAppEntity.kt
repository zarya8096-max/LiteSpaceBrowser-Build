package com.litespace.browser.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.litespace.browser.domain.model.DisplayMode
import com.litespace.browser.domain.model.LiteAppCategory
import com.litespace.browser.domain.model.LiteAppConfig
import com.litespace.browser.domain.model.OrientationMode
import com.litespace.browser.domain.model.PermissionSetting
import com.litespace.browser.domain.model.PopupPolicy
import com.litespace.browser.domain.model.UserAgentPreset

@Entity(
    tableName = "lite_apps",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["profileId"])
    ]
)
data class LiteAppEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val uuid: String,
    val name: String,
    val startUrl: String,
    val category: String,
    val iconText: String,
    val iconColorHex: String,
    val customIconUri: String?,
    val profileId: String,

    val displayMode: String,
    val orientationMode: String,
    val themeColorHex: String,
    val forceDarkWeb: Boolean,
    val pullToRefreshEnabled: Boolean,
    val navigationGesturesEnabled: Boolean,
    val showBottomBar: Boolean,

    val javaScriptEnabled: Boolean,
    val domStorageEnabled: Boolean,
    val desktopMode: Boolean,
    val userAgentPreset: String,
    val customUserAgent: String,
    val textZoomPercent: Int,
    val defaultPageZoomPercent: Int,
    val enableZoomControls: Boolean,

    val customCss: String,
    val customJs: String,
    val injectAtDocumentStart: Boolean,

    val adBlockEnabled: Boolean,
    val blockThirdPartyCookies: Boolean,
    val acceptCookies: Boolean,
    val httpsUpgradeEnabled: Boolean,
    val clearCacheOnExit: Boolean,
    val clearCookiesOnExit: Boolean,
    val clearStorageOnExit: Boolean,
    val openExternalLinksInBrowser: Boolean,
    val popupPolicy: String,
    val biometricLockEnabled: Boolean,

    val allowGeolocation: String,
    val allowCamera: String,
    val allowMicrophone: String,
    val allowWebNotifications: String,

    val feedMonitorUrl: String,
    val feedCheckIntervalMinutes: Int,
    val lastSeenFeedItemHash: String,

    val createdAt: Long,
    val lastLaunchedAt: Long,
    val launchCount: Int,
    val isFavorite: Boolean
) {
    fun toDomain(): LiteAppConfig = LiteAppConfig(
        id = id,
        uuid = uuid,
        name = name,
        startUrl = startUrl,
        category = runCatching { LiteAppCategory.valueOf(category) }.getOrDefault(LiteAppCategory.PRODUCTIVITY),
        iconText = iconText,
        iconColorHex = iconColorHex,
        customIconUri = customIconUri,
        profileId = profileId,
        displayMode = runCatching { DisplayMode.valueOf(displayMode) }.getOrDefault(DisplayMode.STANDALONE),
        orientationMode = runCatching { OrientationMode.valueOf(orientationMode) }.getOrDefault(OrientationMode.SYSTEM),
        themeColorHex = themeColorHex,
        forceDarkWeb = forceDarkWeb,
        pullToRefreshEnabled = pullToRefreshEnabled,
        navigationGesturesEnabled = navigationGesturesEnabled,
        showBottomBar = showBottomBar,
        javaScriptEnabled = javaScriptEnabled,
        domStorageEnabled = domStorageEnabled,
        desktopMode = desktopMode,
        userAgentPreset = runCatching { UserAgentPreset.valueOf(userAgentPreset) }.getOrDefault(UserAgentPreset.ANDROID_MOBILE),
        customUserAgent = customUserAgent,
        textZoomPercent = textZoomPercent,
        defaultPageZoomPercent = defaultPageZoomPercent,
        enableZoomControls = enableZoomControls,
        customCss = customCss,
        customJs = customJs,
        injectAtDocumentStart = injectAtDocumentStart,
        adBlockEnabled = adBlockEnabled,
        blockThirdPartyCookies = blockThirdPartyCookies,
        acceptCookies = acceptCookies,
        httpsUpgradeEnabled = httpsUpgradeEnabled,
        clearCacheOnExit = clearCacheOnExit,
        clearCookiesOnExit = clearCookiesOnExit,
        clearStorageOnExit = clearStorageOnExit,
        openExternalLinksInBrowser = openExternalLinksInBrowser,
        popupPolicy = runCatching { PopupPolicy.valueOf(popupPolicy) }.getOrDefault(PopupPolicy.ALLOW_SAME_ORIGIN),
        biometricLockEnabled = biometricLockEnabled,
        allowGeolocation = runCatching { PermissionSetting.valueOf(allowGeolocation) }.getOrDefault(PermissionSetting.DENY),
        allowCamera = runCatching { PermissionSetting.valueOf(allowCamera) }.getOrDefault(PermissionSetting.DENY),
        allowMicrophone = runCatching { PermissionSetting.valueOf(allowMicrophone) }.getOrDefault(PermissionSetting.DENY),
        allowWebNotifications = runCatching { PermissionSetting.valueOf(allowWebNotifications) }.getOrDefault(PermissionSetting.ASK),
        feedMonitorUrl = feedMonitorUrl,
        feedCheckIntervalMinutes = feedCheckIntervalMinutes,
        lastSeenFeedItemHash = lastSeenFeedItemHash,
        createdAt = createdAt,
        lastLaunchedAt = lastLaunchedAt,
        launchCount = launchCount,
        isFavorite = isFavorite
    )

    companion object {
        fun fromDomain(config: LiteAppConfig): LiteAppEntity = LiteAppEntity(
            id = config.id,
            uuid = config.uuid,
            name = config.name,
            startUrl = config.startUrl,
            category = config.category.name,
            iconText = config.iconText,
            iconColorHex = config.iconColorHex,
            customIconUri = config.customIconUri,
            profileId = config.profileId,
            displayMode = config.displayMode.name,
            orientationMode = config.orientationMode.name,
            themeColorHex = config.themeColorHex,
            forceDarkWeb = config.forceDarkWeb,
            pullToRefreshEnabled = config.pullToRefreshEnabled,
            navigationGesturesEnabled = config.navigationGesturesEnabled,
            showBottomBar = config.showBottomBar,
            javaScriptEnabled = config.javaScriptEnabled,
            domStorageEnabled = config.domStorageEnabled,
            desktopMode = config.desktopMode,
            userAgentPreset = config.userAgentPreset.name,
            customUserAgent = config.customUserAgent,
            textZoomPercent = config.textZoomPercent,
            defaultPageZoomPercent = config.defaultPageZoomPercent,
            enableZoomControls = config.enableZoomControls,
            customCss = config.customCss,
            customJs = config.customJs,
            injectAtDocumentStart = config.injectAtDocumentStart,
            adBlockEnabled = config.adBlockEnabled,
            blockThirdPartyCookies = config.blockThirdPartyCookies,
            acceptCookies = config.acceptCookies,
            httpsUpgradeEnabled = config.httpsUpgradeEnabled,
            clearCacheOnExit = config.clearCacheOnExit,
            clearCookiesOnExit = config.clearCookiesOnExit,
            clearStorageOnExit = config.clearStorageOnExit,
            openExternalLinksInBrowser = config.openExternalLinksInBrowser,
            popupPolicy = config.popupPolicy.name,
            biometricLockEnabled = config.biometricLockEnabled,
            allowGeolocation = config.allowGeolocation.name,
            allowCamera = config.allowCamera.name,
            allowMicrophone = config.allowMicrophone.name,
            allowWebNotifications = config.allowWebNotifications.name,
            feedMonitorUrl = config.feedMonitorUrl,
            feedCheckIntervalMinutes = config.feedCheckIntervalMinutes,
            lastSeenFeedItemHash = config.lastSeenFeedItemHash,
            createdAt = config.createdAt,
            lastLaunchedAt = config.lastLaunchedAt,
            launchCount = config.launchCount,
            isFavorite = config.isFavorite
        )
    }
}
