package com.litespace.browser

import com.litespace.browser.data.preferences.GlobalPreferencesRepository
import com.litespace.browser.data.repository.LiteAppRepository
import com.litespace.browser.domain.model.LiteAppConfig
import com.litespace.browser.domain.model.UserAgentPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class LiteSpaceUnitTest {

    @Test
    fun normalizeUrl_addsHttpsSchemeForBareDomain() {
        val normalized = LiteAppRepository.normalizeUrl("news.ycombinator.com")
        assertEquals("https://news.ycombinator.com", normalized)
    }

    @Test
    fun normalizeUrl_convertsPlainSearchQueryToDuckDuckGoUrl() {
        val normalized = LiteAppRepository.normalizeUrl("kotlin coroutines guide")
        assertTrue(normalized.startsWith("https://duckduckgo.com/?q="))
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
    fun sha256PinHash_isDeterministicAnd64HexChars() {
        val hash1 = GlobalPreferencesRepository.sha256("2468")
        val hash2 = GlobalPreferencesRepository.sha256("2468")
        assertEquals(hash1, hash2)
        assertEquals(64, hash1.length)
    }
}
