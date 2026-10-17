package com.litespace.browser.data.repository

import com.litespace.browser.data.local.LiteSpaceDatabase
import com.litespace.browser.data.local.entity.BookmarkEntity
import com.litespace.browser.data.local.entity.ContentFilterRuleEntity
import com.litespace.browser.data.local.entity.LiteAppEntity
import com.litespace.browser.data.preferences.GlobalPreferencesRepository
import com.litespace.browser.domain.model.BookmarkItem
import com.litespace.browser.domain.model.ContentFilterRule
import com.litespace.browser.domain.model.LiteAppConfig
import com.litespace.browser.domain.model.LiteSpaceBackupPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
        val normalizedUrl = normalizeUrl(config.startUrl)
        val normalizedConfig = config.copy(
            startUrl = normalizedUrl,
            iconText = config.iconText.ifBlank {
                config.name.trim().take(2).uppercase().ifBlank { "LA" }
            }
        )
        return liteAppDao.insertOrReplace(LiteAppEntity.fromDomain(normalizedConfig))
    }

    suspend fun deleteLiteApp(id: Long) {
        bookmarkDao.deleteBookmarksForApp(id)
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

    suspend fun exportBackupJson(): String {
        val payload = LiteSpaceBackupPayload(
            schemaVersion = 1,
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
        val appEntities = payload.liteApps.map { LiteAppEntity.fromDomain(it) }
        liteAppDao.insertAll(appEntities)
        val ruleEntities = payload.customFilterRules.map { ContentFilterRuleEntity.fromDomain(it) }
        contentFilterDao.insertAll(ruleEntities)
        val bookmarkEntities = payload.bookmarks.map { BookmarkEntity.fromDomain(it) }
        bookmarkDao.insertAll(bookmarkEntities)
        appEntities.size
    }

    companion object {
        fun normalizeUrl(raw: String): String {
            val trimmed = raw.trim()
            if (trimmed.isBlank()) return "https://example.com"
            return if (
                trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("about:", ignoreCase = true)
            ) {
                trimmed
            } else if (trimmed.contains(".") && !trimmed.contains(" ")) {
                "https://$trimmed"
            } else {
                "https://duckduckgo.com/?q=${java.net.URLEncoder.encode(trimmed, "UTF-8")}"
            }
        }
    }
}
