package com.litespace.browser.data.repository

import android.net.Uri
import com.litespace.browser.data.local.LiteSpaceDatabase
import com.litespace.browser.data.local.entity.BookmarkEntity
import com.litespace.browser.data.local.entity.ContentFilterRuleEntity
import com.litespace.browser.data.local.entity.HistoryEntity
import com.litespace.browser.data.local.entity.LiteAppEntity
import com.litespace.browser.data.preferences.GlobalPreferencesRepository
import com.litespace.browser.domain.model.BookmarkItem
import com.litespace.browser.domain.model.ContentFilterRule
import com.litespace.browser.domain.model.HistoryItem
import com.litespace.browser.domain.model.LiteAppConfig
import com.litespace.browser.domain.model.LiteSpaceBackupPayload
import com.litespace.browser.domain.model.SearchEnginePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class LiteAppRepository(
    private val database: LiteSpaceDatabase,
    val preferencesRepository: GlobalPreferencesRepository
) {
    private val liteAppDao = database.liteAppDao()
    private val contentFilterDao = database.contentFilterDao()
    private val bookmarkDao = database.bookmarkDao()
    val cookieVaultDao = database.cookieVaultDao()

    private val jsonCodec = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val allLiteAppsFlow: Flow<List<LiteAppConfig>> =
        liteAppDao.observeAllLiteApps().map { list -> list.map { it.toDomain() } }

    val allFilterRulesFlow: Flow<List<ContentFilterRule>> =
        contentFilterDao.observeAllRules().map { list -> list.map { it.toDomain() } }

    fun observeLiteAppById(id: Long): Flow<LiteAppConfig?> =
        liteAppDao.observeLiteAppById(id).map { it?.toDomain() }

    suspend fun getLiteAppById(id: Long): LiteAppConfig? =
        liteAppDao.getLiteAppById(id)?.toDomain()

    suspend fun saveLiteApp(config: LiteAppConfig): Long {
        val rawUrlCandidate = config.startUrl.trim()
        val effectiveUrlInput = if (rawUrlCandidate.isBlank() ||
            rawUrlCandidate.equals("https://", ignoreCase = true) ||
            rawUrlCandidate.equals("http://", ignoreCase = true)
        ) {
            config.name.trim().ifBlank { "https://duckduckgo.com" }
        } else {
            rawUrlCandidate
        }

        val normalizedUrl = normalizeUrl(effectiveUrlInput)
        val derivedName = config.name.trim().ifBlank {
            extractHostLabel(normalizedUrl)
        }
        val derivedMonogram = config.iconText.trim().take(2).uppercase().let { mono ->
            if (mono.isBlank() || (mono == "LA" && derivedName.isNotBlank())) {
                derivedName.filter { it.isLetterOrDigit() }.take(2).uppercase().ifBlank { "LA" }
            } else {
                mono
            }
        }

        val normalizedConfig = config.copy(
            uuid = config.uuid.ifBlank { UUID.randomUUID().toString() },
            name = derivedName,
            startUrl = normalizedUrl,
            iconText = derivedMonogram,
            profileId = config.profileId.trim().ifBlank { "default" }
        )
        return liteAppDao.insertOrReplace(LiteAppEntity.fromDomain(normalizedConfig))
    }

    suspend fun deleteLiteApp(id: Long) {
        bookmarkDao.deleteBookmarksForApp(id)
        bookmarkDao.clearHistoryForApp(id)
        liteAppDao.deleteById(id)
    }

    suspend fun recordAppLaunch(id: Long) {
        liteAppDao.recordLaunch(id)
    }

    fun observeBookmarks(liteAppId: Long): Flow<List<BookmarkItem>> =
        bookmarkDao.observeBookmarksForApp(liteAppId).map { list -> list.map { it.toDomain() } }

    suspend fun addBookmark(liteAppId: Long, title: String, url: String): Long {
        return bookmarkDao.insertBookmark(
            BookmarkEntity(
                liteAppId = liteAppId,
                title = title.ifBlank { url },
                url = url
            )
        )
    }

    suspend fun deleteBookmark(id: Long) {
        bookmarkDao.deleteBookmarkById(id)
    }

    fun observeHistory(liteAppId: Long): Flow<List<HistoryItem>> =
        bookmarkDao.observeHistoryForApp(liteAppId).map { list -> list.map { it.toDomain() } }

    suspend fun recordHistoryVisit(liteAppId: Long, title: String, url: String) {
        if (liteAppId <= 0L || url.isBlank() || url == "about:blank") return
        bookmarkDao.insertHistoryEntry(
            HistoryEntity(
                liteAppId = liteAppId,
                title = title.ifBlank { url },
                url = url
            )
        )
    }

    suspend fun clearHistoryForApp(liteAppId: Long) {
        bookmarkDao.clearHistoryForApp(liteAppId)
    }

    suspend fun getEnabledFilterRules(): List<ContentFilterRule> =
        contentFilterDao.getEnabledRulesOnce().map { it.toDomain() }

    suspend fun addFilterRule(pattern: String, isException: Boolean): Long {
        val cleaned = pattern.trim().lowercase()
        if (cleaned.isBlank()) return -1L
        return contentFilterDao.insertRule(
            ContentFilterRuleEntity(
                pattern = cleaned,
                isException = isException,
                isEnabled = true
            )
        )
    }

    suspend fun toggleFilterRule(id: Long, enabled: Boolean) {
        contentFilterDao.setRuleEnabled(id, enabled)
    }

    suspend fun deleteFilterRule(id: Long) {
        contentFilterDao.deleteRuleById(id)
    }

    suspend fun recordFilterRuleHit(pattern: String) {
        contentFilterDao.incrementHitCount(pattern)
        preferencesRepository.incrementBlockedRequests(1L)
    }

    fun exportSingleLiteAppJson(config: LiteAppConfig): String {
        return jsonCodec.encodeToString(config.copy(id = 0L))
    }

    suspend fun importSingleLiteAppJson(rawJson: String): Result<Long> = runCatching {
        val decoded = jsonCodec.decodeFromString<LiteAppConfig>(rawJson)
        saveLiteApp(decoded.copy(id = 0L, uuid = UUID.randomUUID().toString()))
    }

    suspend fun exportBackupJson(): String {
        val payload = LiteSpaceBackupPayload(
            schemaVersion = 2,
            exportedAtEpochMs = System.currentTimeMillis(),
            globalSettings = preferencesRepository.getSettingsOnce(),
            liteApps = liteAppDao.getAllLiteAppsOnce().map { it.toDomain() },
            customFilterRules = contentFilterDao.getAllRulesOnce().map { it.toDomain() },
            bookmarks = bookmarkDao.getAllBookmarksOnce().map { it.toDomain() }
        )
        return jsonCodec.encodeToString(payload)
    }

    suspend fun importBackupJson(rawJson: String): Result<Int> = runCatching {
        val payload = jsonCodec.decodeFromString<LiteSpaceBackupPayload>(rawJson)
        preferencesRepository.restoreGlobalSettings(payload.globalSettings)
        val appEntities = payload.liteApps.map {
            LiteAppEntity.fromDomain(
                it.copy(uuid = it.uuid.ifBlank { UUID.randomUUID().toString() })
            )
        }
        liteAppDao.insertAll(appEntities)
        val ruleEntities = payload.customFilterRules.map { ContentFilterRuleEntity.fromDomain(it) }
        contentFilterDao.insertAll(ruleEntities)
        val bookmarkEntities = payload.bookmarks.map { BookmarkEntity.fromDomain(it) }
        bookmarkDao.insertAll(bookmarkEntities)
        appEntities.size
    }

    companion object {
        fun normalizeUrl(
            raw: String,
            engine: SearchEnginePreset = SearchEnginePreset.DUCKDUCKGO
        ): String {
            var cleaned = raw.trim()
            while (cleaned.startsWith("https://https://", ignoreCase = true)) {
                cleaned = "https://" + cleaned.substring("https://https://".length)
            }
            while (cleaned.startsWith("http://https://", ignoreCase = true)) {
                cleaned = "https://" + cleaned.substring("http://https://".length)
            }
            while (cleaned.startsWith("https://http://", ignoreCase = true)) {
                cleaned = "http://" + cleaned.substring("https://http://".length)
            }

            if (cleaned.isBlank() ||
                cleaned.equals("https://", ignoreCase = true) ||
                cleaned.equals("http://", ignoreCase = true)
            ) {
                return "https://duckduckgo.com"
            }

            return if (
                cleaned.startsWith("https://", ignoreCase = true) ||
                cleaned.startsWith("http://", ignoreCase = true) ||
                cleaned.startsWith("about:", ignoreCase = true)
            ) {
                cleaned
            } else if (cleaned.contains(".") && !cleaned.contains(" ")) {
                "https://$cleaned"
            } else {
                "${engine.queryUrlPrefix}${java.net.URLEncoder.encode(cleaned, "UTF-8")}"
            }
        }

        fun extractHostLabel(url: String): String {
            val host = runCatching { Uri.parse(url).host.orEmpty() }.getOrDefault("")
                .removePrefix("www.")
                .removePrefix("m.")
                .removePrefix("en.")
                .removePrefix("fa.")
            val mainPart = host.substringBefore('.').trim()
            return if (mainPart.isNotBlank()) {
                mainPart.replaceFirstChar { it.uppercase() }
            } else {
                "Lite App"
            }
        }
    }
}
