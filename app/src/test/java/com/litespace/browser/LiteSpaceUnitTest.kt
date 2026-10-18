package com.litespace.browser

import com.litespace.browser.data.preferences.GlobalPreferencesRepository
import com.litespace.browser.data.repository.LiteAppRepository
import com.litespace.browser.domain.model.CatalogTemplates
import com.litespace.browser.domain.model.ContentFilterRule
import com.litespace.browser.domain.model.GlobalAppSettings
import com.litespace.browser.domain.model.LiteAppConfig
import com.litespace.browser.domain.model.LiteSpaceBackupPayload
import com.litespace.browser.domain.model.SearchEnginePreset
import com.litespace.browser.domain.model.UserAgentPreset
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class LiteSpaceUnitTest {

    @Test
    fun normalizeUrl_addsHttpsSchemeForBareDomain() {
        assertEquals("https://news.ycombinator.com", LiteAppRepository.normalizeUrl("news.ycombinator.com"))
        assertEquals("https://en.m.wikipedia.org/wiki/Kotlin", LiteAppRepository.normalizeUrl("en.m.wikipedia.org/wiki/Kotlin"))
    }

    @Test
    fun normalizeUrl_preservesExistingHttpOrHttpsScheme() {
        assertEquals("http://localhost:8080/test", LiteAppRepository.normalizeUrl("http://localhost:8080/test"))
        assertEquals("https://github.com", LiteAppRepository.normalizeUrl("  https://github.com  "))
    }

    @Test
    fun normalizeUrl_convertsPlainSearchQueryToDuckDuckGoUrl() {
        val normalized = LiteAppRepository.normalizeUrl("kotlin coroutines guide")
        assertTrue(normalized.startsWith("https://duckduckgo.com/?q="))
        assertTrue(normalized.contains("kotlin+coroutines+guide"))
    }

    @Test
    fun resolvedUserAgent_switchesToDesktopWhenDesktopModeEnabled() {
        val config = LiteAppConfig(
            uuid = UUID.randomUUID().toString(),
            name = "Test App",
            startUrl = "https://example.com",
            desktopMode = true,
            userAgentPreset = UserAgentPreset.ANDROID_MOBILE
        )
        assertEquals(UserAgentPreset.DESKTOP_CHROME.userAgentString, config.resolvedUserAgent())
    }

    @Test
    fun resolvedUserAgent_usesCustomUserAgentWhenCustomPresetSelected() {
        val customUa = "MyCustomAgent/2.5 (Android; LiteSpace)"
        val config = LiteAppConfig(
            uuid = UUID.randomUUID().toString(),
            name = "Custom UA App",
            startUrl = "https://example.com",
            userAgentPreset = UserAgentPreset.CUSTOM,
            customUserAgent = customUa
        )
        assertEquals(customUa, config.resolvedUserAgent())
    }

    @Test
    fun sha256PinHash_isDeterministicAnd64HexChars() {
        val hash1 = GlobalPreferencesRepository.sha256("2468")
        val hash2 = GlobalPreferencesRepository.sha256("2468")
        val hashDifferent = GlobalPreferencesRepository.sha256("1357")
        assertEquals(hash1, hash2)
        assertEquals(64, hash1.length)
        assertFalse(hash1 == hashDifferent)
    }

    @Test
    fun backupPayload_jsonRoundTripPreservesAllAppsAndRules() {
        val jsonCodec = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
        val original = LiteSpaceBackupPayload(
            schemaVersion = 2,
            globalSettings = GlobalAppSettings(languageCode = "fa", totalBlockedRequests = 42L),
            liteApps = listOf(
                LiteAppConfig(
                    id = 7L,
                    uuid = "uuid-7",
                    name = "ویکی‌پدیا",
                    startUrl = "https://fa.m.wikipedia.org",
                    profileId = "work",
                    forceDarkWeb = true
                )
            ),
            customFilterRules = listOf(
                ContentFilterRule(id = 1L, pattern = "ads.example.com", isException = false)
            ),
            bookmarks = emptyList()
        )
        val encoded = jsonCodec.encodeToString(original)
        val decoded = jsonCodec.decodeFromString<LiteSpaceBackupPayload>(encoded)
        assertEquals("fa", decoded.globalSettings.languageCode)
        assertEquals(42L, decoded.globalSettings.totalBlockedRequests)
        assertEquals(1, decoded.liteApps.size)
        assertEquals("ویکی‌پدیا", decoded.liteApps[0].name)
        assertEquals("work", decoded.liteApps[0].profileId)
    }

    @Test
    fun builtInCatalogAndExtensions_areNonEmptyAndValid() {
        assertTrue(CatalogTemplates.builtInTemplates.size >= 6)
        assertTrue(CatalogTemplates.builtInExtensions.size >= 5)
        CatalogTemplates.builtInTemplates.forEach { tpl ->
            assertTrue(tpl.startUrl.startsWith("https://"))
            assertTrue(tpl.name.isNotBlank())
        }
    }

    @Test
    fun normalizeUrl_supportsMultipleSearchEnginePresets() {
        val braveUrl = LiteAppRepository.normalizeUrl("android compose", SearchEnginePreset.BRAVE)
        assertTrue(braveUrl.startsWith("https://search.brave.com/search?q="))
        val googleUrl = LiteAppRepository.normalizeUrl("android compose", SearchEnginePreset.GOOGLE)
        assertTrue(googleUrl.startsWith("https://www.google.com/search?q="))
    }

    @Test
    fun normalizeUrl_handlesBareSchemeAndDuplicateHttpsSafely() {
        assertEquals("https://duckduckgo.com", LiteAppRepository.normalizeUrl("https://"))
        assertEquals("https://duckduckgo.com", LiteAppRepository.normalizeUrl("   "))
        assertEquals("https://digikala.com", LiteAppRepository.normalizeUrl("https://https://digikala.com"))
        assertEquals("Digikala", LiteAppRepository.extractHostLabel("https://www.digikala.com/product/1"))
    }
}
