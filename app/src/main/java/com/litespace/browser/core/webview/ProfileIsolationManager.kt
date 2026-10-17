package com.litespace.browser.core.webview

import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import com.litespace.browser.data.local.dao.CookieVaultDao
import com.litespace.browser.data.local.entity.CookieVaultEntity
import com.litespace.browser.data.preferences.GlobalPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject

class ProfileIsolationManager(
    private val cookieVaultDao: CookieVaultDao,
    private val preferencesRepository: GlobalPreferencesRepository
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun switchToProfile(
        targetProfileId: String,
        targetUrl: String,
        isIncognito: Boolean
    ) = withContext(Dispatchers.IO) {
        val cookieManager = CookieManager.getInstance()
        if (isIncognito) {
            withContext(Dispatchers.Main) {
                cookieManager.removeAllCookies(null)
                cookieManager.flush()
            }
            return@withContext
        }

        val currentProfileId = preferencesRepository.getActiveProfileId()
        if (currentProfileId != targetProfileId) {
            withContext(Dispatchers.Main) {
                cookieManager.removeAllCookies(null)
            }
            val savedEntries = cookieVaultDao.getVaultEntriesForProfile(targetProfileId)
            withContext(Dispatchers.Main) {
                for (entry in savedEntries) {
                    val cookies = runCatching {
                        json.decodeFromString<List<String>>(entry.serializedCookiesJson)
                    }.getOrDefault(emptyList())
                    val originUrl = "https://${entry.host}"
                    cookies.forEach { cookiePair ->
                        cookieManager.setCookie(originUrl, cookiePair)
                    }
                }
                cookieManager.flush()
            }
            preferencesRepository.setActiveProfileId(targetProfileId)
        }
    }

    fun captureWebViewProfileStateAsync(
        webView: WebView?,
        profileId: String,
        currentUrl: String,
        isIncognito: Boolean
    ) {
        if (isIncognito || profileId.isBlank() || profileId == "incognito") return
        val safeUrl = currentUrl.takeIf { it.startsWith("http") } ?: return
        val cookieHeader = runCatching {
            CookieManager.getInstance().getCookie(safeUrl).orEmpty()
        }.getOrDefault("")

        if (webView == null) {
            ioScope.launch {
                snapshotCurrentProfileCookies(profileId, safeUrl, "{}", cookieHeader)
            }
            return
        }

        val dumpScript = """
            (function() {
                try {
                    var obj = {};
                    for (var i = 0; i < window.localStorage.length; i++) {
                        var k = window.localStorage.key(i);
                        if (k) obj[k] = window.localStorage.getItem(k);
                    }
                    return JSON.stringify(obj);
                } catch (e) {
                    return "{}";
                }
            })();
        """.trimIndent()

        runCatching {
            webView.evaluateJavascript(dumpScript) { rawEvalResult ->
                val unquotedJson = unquoteJsEvalString(rawEvalResult)
                ioScope.launch {
                    snapshotCurrentProfileCookies(profileId, safeUrl, unquotedJson, cookieHeader)
                }
            }
        }.onFailure {
            ioScope.launch {
                snapshotCurrentProfileCookies(profileId, safeUrl, "{}", cookieHeader)
            }
        }
    }

    private fun unquoteJsEvalString(raw: String?): String {
        if (raw.isNullOrBlank() || raw == "null") return "{}"
        return runCatching {
            if (raw.startsWith("\"") && raw.endsWith("\"")) {
                org.json.JSONTokener(raw).nextValue().toString()
            } else {
                raw
            }
        }.getOrDefault("{}")
    }

    suspend fun snapshotCurrentProfileCookies(
        profileId: String,
        currentUrl: String,
        localStorageDumpJson: String = "{}",
        preFetchedCookieHeader: String? = null
    ) = withContext(Dispatchers.IO) {
        if (profileId.isBlank() || profileId == "incognito") return@withContext
        val host = runCatching { Uri.parse(currentUrl).host?.lowercase() }.getOrNull() ?: return@withContext
        val cookieHeader = preFetchedCookieHeader ?: withContext(Dispatchers.Main) {
            runCatching { CookieManager.getInstance().getCookie(currentUrl).orEmpty() }.getOrDefault("")
        }
        val cookiePairs = cookieHeader
            .split(";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val vaultKey = "$profileId::$host"
        val existing = cookieVaultDao.getVaultEntry(vaultKey)
        val mergedLocalStorage = if (localStorageDumpJson.isNotBlank() &&
            localStorageDumpJson != "{}" &&
            runCatching { JSONObject(localStorageDumpJson) }.isSuccess
        ) {
            localStorageDumpJson
        } else {
            existing?.localStorageJson ?: "{}"
        }

        cookieVaultDao.upsertVaultEntry(
            CookieVaultEntity(
                vaultKey = vaultKey,
                profileId = profileId,
                host = host,
                serializedCookiesJson = json.encodeToString(cookiePairs),
                localStorageJson = mergedLocalStorage,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun buildLocalStorageRestoreScript(profileId: String, currentUrl: String): String? =
        withContext<String?>(Dispatchers.IO) {
            if (profileId == "incognito") return@withContext null
            val host = runCatching { Uri.parse(currentUrl).host?.lowercase() }.getOrNull() ?: return@withContext null
            val entry = cookieVaultDao.getVaultEntry("$profileId::$host") ?: return@withContext null
            if (entry.localStorageJson.isBlank() || entry.localStorageJson == "{}") return@withContext null

            val escapedJson = entry.localStorageJson
                .replace("\\", "\\\\")
                .replace("'", "\\'")
            """
            (function() {
                try {
                    var data = JSON.parse('$escapedJson');
                    Object.keys(data).forEach(function(k) {
                        if (window.localStorage.getItem(k) === null) {
                            window.localStorage.setItem(k, data[k]);
                        }
                    });
                } catch (e) {}
            })();
            """.trimIndent()
        }

    fun clearSessionData(
        webView: WebView?,
        clearCache: Boolean,
        clearCookies: Boolean,
        clearStorage: Boolean,
        isIncognito: Boolean
    ) {
        if (clearCache || isIncognito) {
            runCatching {
                webView?.clearCache(true)
                webView?.clearHistory()
                webView?.clearFormData()
            }
        }
        if (clearCookies || isIncognito) {
            runCatching {
                val cookieManager = CookieManager.getInstance()
                cookieManager.removeAllCookies(null)
                cookieManager.flush()
            }
        }
        if (clearStorage || isIncognito) {
            runCatching {
                WebStorage.getInstance().deleteAllData()
                webView?.evaluateJavascript(
                    "try { window.localStorage.clear(); window.sessionStorage.clear(); } catch(e) {}",
                    null
                )
            }
        }
    }
}
