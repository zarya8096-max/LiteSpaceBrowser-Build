package com.litespace.browser.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class LiteAppCategory(val labelEn: String, val labelFa: String) {
    SOCIAL("Social", "اجتماعی"),
    NEWS("News & Reading", "اخبار و مطالعه"),
    PRODUCTIVITY("Productivity", "بهره‌وری"),
    DEVELOPER("Developer", "توسعه‌دهندگان"),
    SHOPPING("Shopping", "خرید"),
    MEDIA("Media", "رسانه")
}

@Serializable
enum class DisplayMode(val labelEn: String, val labelFa: String) {
    STANDALONE("Standalone App Bar", "نوار ابزار استاندارد"),
    FRAMELESS("Frameless (Pull Toolbar)", "بدون قاب (تمام‌صفحه)"),
    FULLSCREEN_IMMERSIVE("Fullscreen Immersive (Hide System Bars)", "غوطه‌ور کامل (مخفی کردن نوار سیستم)")
}

@Serializable
enum class OrientationMode(val labelEn: String, val labelFa: String) {
    SYSTEM("System Default", "پیش‌فرض سیستم"),
    PORTRAIT("Lock Portrait", "قفل عمودی"),
    LANDSCAPE("Lock Landscape", "قفل افقی")
}

@Serializable
enum class UserAgentPreset(val labelEn: String, val userAgentString: String) {
    ANDROID_MOBILE(
        "Android Chrome Mobile (Default)",
        "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
    ),
    DESKTOP_CHROME(
        "Desktop Chrome (Linux / x86_64)",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
    ),
    DESKTOP_FIREFOX(
        "Desktop Firefox",
        "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:130.0) Gecko/20100101 Firefox/130.0"
    ),
    SAFARI_IOS(
        "iPhone Safari Mobile",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.6 Mobile/15E148 Safari/604.1"
    ),
    CUSTOM(
        "Custom User-Agent String",
        ""
    )
}

@Serializable
enum class PopupPolicy(val labelEn: String, val labelFa: String) {
    BLOCK_ALL("Block All Pop-ups", "مسدودسازی همه پاپ‌آپ‌ها"),
    ALLOW_SAME_ORIGIN("Allow Same-Domain Only", "فقط هم‌دامنه"),
    ALLOW_ALL("Allow Pop-ups in New Sheet", "مجاز برای همه")
}

@Serializable
enum class PermissionSetting(val labelEn: String, val labelFa: String) {
    DENY("Always Deny", "همیشه رد شود"),
    ASK("Ask Every Time", "هر بار پرسیده شود"),
    ALLOW("Allow When Requested", "مجاز در صورت درخواست")
}

@Serializable
enum class AppThemeMode(val labelEn: String, val labelFa: String) {
    SYSTEM("Follow System", "پیروی از سیستم"),
    LIGHT("Light Theme", "پوسته روشن"),
    DARK("Dark Theme", "پوسته تیره"),
    AMOLED("AMOLED Pure Black", "مشکی خالص AMOLED")
}

@Serializable
data class LiteAppConfig(
    val id: Long = 0L,
    val uuid: String,
    val name: String,
    val startUrl: String,
    val category: LiteAppCategory = LiteAppCategory.PRODUCTIVITY,
    val iconText: String = "LA",
    val iconColorHex: String = "#0B57D0",
    val customIconUri: String? = null,
    val profileId: String = "default",

    val displayMode: DisplayMode = DisplayMode.STANDALONE,
    val orientationMode: OrientationMode = OrientationMode.SYSTEM,
    val themeColorHex: String = "#0B57D0",
    val forceDarkWeb: Boolean = false,
    val pullToRefreshEnabled: Boolean = true,
    val navigationGesturesEnabled: Boolean = true,
    val showBottomBar: Boolean = true,

    val javaScriptEnabled: Boolean = true,
    val domStorageEnabled: Boolean = true,
    val desktopMode: Boolean = false,
    val userAgentPreset: UserAgentPreset = UserAgentPreset.ANDROID_MOBILE,
    val customUserAgent: String = "",
    val textZoomPercent: Int = 100,
    val defaultPageZoomPercent: Int = 100,
    val enableZoomControls: Boolean = true,

    val customCss: String = "",
    val customJs: String = "",
    val injectAtDocumentStart: Boolean = false,

    val adBlockEnabled: Boolean = true,
    val blockThirdPartyCookies: Boolean = true,
    val acceptCookies: Boolean = true,
    val httpsUpgradeEnabled: Boolean = true,
    val clearCacheOnExit: Boolean = false,
    val clearCookiesOnExit: Boolean = false,
    val clearStorageOnExit: Boolean = false,
    val openExternalLinksInBrowser: Boolean = false,
    val popupPolicy: PopupPolicy = PopupPolicy.ALLOW_SAME_ORIGIN,
    val biometricLockEnabled: Boolean = false,

    val allowGeolocation: PermissionSetting = PermissionSetting.DENY,
    val allowCamera: PermissionSetting = PermissionSetting.DENY,
    val allowMicrophone: PermissionSetting = PermissionSetting.DENY,
    val allowWebNotifications: PermissionSetting = PermissionSetting.ASK,

    val feedMonitorUrl: String = "",
    val feedCheckIntervalMinutes: Int = 0,
    val lastSeenFeedItemHash: String = "",

    val createdAt: Long = System.currentTimeMillis(),
    val lastLaunchedAt: Long = 0L,
    val launchCount: Int = 0,
    val isFavorite: Boolean = false
) {
    fun resolvedUserAgent(): String {
        if (desktopMode && userAgentPreset == UserAgentPreset.ANDROID_MOBILE) {
            return UserAgentPreset.DESKTOP_CHROME.userAgentString
        }
        return when (userAgentPreset) {
            UserAgentPreset.CUSTOM -> customUserAgent.ifBlank { UserAgentPreset.ANDROID_MOBILE.userAgentString }
            else -> userAgentPreset.userAgentString
        }
    }
}

@Serializable
data class ContentFilterRule(
    val id: Long = 0L,
    val pattern: String,
    val isException: Boolean = false,
    val isEnabled: Boolean = true,
    val hitCount: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class BookmarkItem(
    val id: Long = 0L,
    val liteAppId: Long,
    val title: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class GlobalAppSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val languageCode: String = "en",
    val globalAdBlockEnabled: Boolean = true,
    val globalHttpsOnly: Boolean = true,
    val globalBlockThirdPartyCookies: Boolean = true,
    val globalClearOnExit: Boolean = false,
    val biometricGlobalEnabled: Boolean = false,
    val pinCodeSha256: String = "",
    val totalBlockedRequests: Long = 0L
)

@Serializable
data class LiteSpaceBackupPayload(
    val schemaVersion: Int = 1,
    val exportedAtEpochMs: Long = System.currentTimeMillis(),
    val globalSettings: GlobalAppSettings,
    val liteApps: List<LiteAppConfig>,
    val customFilterRules: List<ContentFilterRule>,
    val bookmarks: List<BookmarkItem>
)

data class CatalogTemplate(
    val name: String,
    val startUrl: String,
    val category: LiteAppCategory,
    val iconText: String,
    val iconColorHex: String,
    val feedUrl: String = "",
    val descriptionEn: String,
    val descriptionFa: String
)
