package com.litespace.browser

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.litespace.browser.core.webview.ContentBlockerEngine
import com.litespace.browser.domain.model.ContentFilterRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiteSpaceInstrumentedTest {

    @Test
    fun contentBlocker_blocksKnownAdDomainAndHonorsExceptionRule() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val blocker = ContentBlockerEngine(context)
        blocker.ensureInitialized(
            listOf(
                ContentFilterRule(pattern = "custom-tracker.example.org", isException = false, isEnabled = true),
                ContentFilterRule(pattern = "allowed.doubleclick.net", isException = true, isEnabled = true)
            )
        )

        val blockedBuiltIn = blocker.matchBlockedRequest("https://ad.doubleclick.net/ddm/ad", "https://news.com")
        assertEquals("doubleclick.net", blockedBuiltIn)

        val allowedException = blocker.matchBlockedRequest("https://allowed.doubleclick.net/pixel.js", "https://news.com")
        assertNull(allowedException)

        val blockedCustom = blocker.matchBlockedRequest("https://sub.custom-tracker.example.org/collect", "https://news.com")
        assertNotNull(blockedCustom)

        // Verify rule reload removes deleted custom rules
        blocker.reloadCustomRules(emptyList())
        val afterRemoval = blocker.matchBlockedRequest("https://sub.custom-tracker.example.org/collect", "https://news.com")
        assertNull(afterRemoval)
    }
}
