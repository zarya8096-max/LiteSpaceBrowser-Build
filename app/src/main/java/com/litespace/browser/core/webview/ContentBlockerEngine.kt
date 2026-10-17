package com.litespace.browser.core.webview

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import com.litespace.browser.domain.model.ContentFilterRule
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap

class ContentBlockerEngine(private val context: Context) {

    private val bundledBlockedDomains = ConcurrentHashMap.newKeySet<String>()
    private val bundledExceptionDomains = ConcurrentHashMap.newKeySet<String>()
    private val bundledBlockedPaths = ConcurrentHashMap.newKeySet<String>()

    private val customBlockedDomains = ConcurrentHashMap.newKeySet<String>()
    private val customExceptionDomains = ConcurrentHashMap.newKeySet<String>()
    private val customBlockedPaths = ConcurrentHashMap.newKeySet<String>()

    @Volatile
    private var isInitialized = false

    fun ensureInitialized(customRules: List<ContentFilterRule> = emptyList()) {
        if (!isInitialized) {
            synchronized(this) {
                if (!isInitialized) {
                    loadBundledFilterList()
                    isInitialized = true
                }
            }
        }
        reloadCustomRules(customRules)
    }

    fun getBundledRuleCount(): Int =
        bundledBlockedDomains.size + bundledBlockedPaths.size

    private fun loadBundledFilterList() {
        runCatching {
            context.assets.open("filters/default_blocklist.txt").bufferedReader().useLines { lines ->
                lines.forEach { rawLine ->
                    parseRuleIntoSets(
                        rawLine = rawLine,
                        isEnabled = true,
                        isExplicitException = false,
                        targetBlockedDomains = bundledBlockedDomains,
                        targetExceptionDomains = bundledExceptionDomains,
                        targetBlockedPaths = bundledBlockedPaths
                    )
                }
            }
        }
    }

    @Synchronized
    fun reloadCustomRules(customRules: List<ContentFilterRule>) {
        customBlockedDomains.clear()
        customExceptionDomains.clear()
        customBlockedPaths.clear()

        customRules.forEach { rule ->
            if (rule.isEnabled) {
                parseRuleIntoSets(
                    rawLine = rule.pattern,
                    isEnabled = true,
                    isExplicitException = rule.isException,
                    targetBlockedDomains = customBlockedDomains,
                    targetExceptionDomains = customExceptionDomains,
                    targetBlockedPaths = customBlockedPaths
                )
            }
        }
    }

    private fun parseRuleIntoSets(
        rawLine: String,
        isEnabled: Boolean,
        isExplicitException: Boolean,
        targetBlockedDomains: MutableSet<String>,
        targetExceptionDomains: MutableSet<String>,
        targetBlockedPaths: MutableSet<String>
    ) {
        if (!isEnabled) return
        val trimmed = rawLine.trim().lowercase()
        if (trimmed.isEmpty() || trimmed.startsWith("!") || trimmed.startsWith("#")) return

        val isException = isExplicitException || trimmed.startsWith("@@")
        val cleaned = trimmed
            .removePrefix("@@")
            .removePrefix("||")
            .removeSuffix("^")
            .removePrefix("https://")
            .removePrefix("http://")
            .trim()

        if (cleaned.isEmpty()) return

        if (isException) {
            targetExceptionDomains.add(cleaned)
        } else if (cleaned.startsWith("/") || cleaned.contains("/")) {
            targetBlockedPaths.add(cleaned)
        } else {
            targetBlockedDomains.add(cleaned)
        }
    }

    fun matchBlockedRequest(requestUrl: String, pageOriginHost: String?): String? {
        val uri = runCatching { Uri.parse(requestUrl) }.getOrNull() ?: return null
        val host = uri.host?.lowercase() ?: return null
        if (host == "appassets.androidplatform.net") return null

        if (findMatchingDomain(host, customExceptionDomains) != null ||
            findMatchingDomain(host, bundledExceptionDomains) != null
        ) {
            return null
        }

        findMatchingDomain(host, customBlockedDomains)?.let { return it }
        findMatchingDomain(host, bundledBlockedDomains)?.let { return it }

        val lowerUrl = requestUrl.lowercase()
        for (fragment in customBlockedPaths) {
            if (lowerUrl.contains(fragment)) return fragment
        }
        for (fragment in bundledBlockedPaths) {
            if (lowerUrl.contains(fragment)) return fragment
        }
        return null
    }

    private fun findMatchingDomain(host: String, domainSet: Set<String>): String? {
        var current = host
        while (current.isNotEmpty()) {
            if (domainSet.contains(current)) {
                return current
            }
            val dotIndex = current.indexOf('.')
            if (dotIndex < 0) break
            current = current.substring(dotIndex + 1)
        }
        return null
    }

    fun createEmptyBlockedResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            204,
            "No Content",
            mapOf(
                "Access-Control-Allow-Origin" to "*",
                "Cache-Control" to "no-store"
            ),
            ByteArrayInputStream(ByteArray(0))
        )
    }
}
