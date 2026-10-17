package com.litespace.browser.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.litespace.browser.domain.model.AppThemeMode
import com.litespace.browser.domain.model.GlobalAppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "litespace_global_prefs")

class GlobalPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val GLOBAL_AD_BLOCK = booleanPreferencesKey("global_ad_block")
        val GLOBAL_HTTPS_ONLY = booleanPreferencesKey("global_https_only")
        val GLOBAL_BLOCK_3P_COOKIES = booleanPreferencesKey("global_block_3p_cookies")
        val GLOBAL_CLEAR_ON_EXIT = booleanPreferencesKey("global_clear_on_exit")
        val BIOMETRIC_GLOBAL = booleanPreferencesKey("biometric_global")
        val PIN_SHA256 = stringPreferencesKey("pin_sha256")
        val TOTAL_BLOCKED_REQUESTS = longPreferencesKey("total_blocked_requests")
        val ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
    }

    val settingsFlow: Flow<GlobalAppSettings> = context.dataStore.data.map { prefs ->
        GlobalAppSettings(
            themeMode = runCatching {
                AppThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: AppThemeMode.SYSTEM.name)
            }.getOrDefault(AppThemeMode.SYSTEM),
            dynamicColorEnabled = prefs[Keys.DYNAMIC_COLOR] ?: true,
            languageCode = prefs[Keys.LANGUAGE_CODE] ?: "en",
            globalAdBlockEnabled = prefs[Keys.GLOBAL_AD_BLOCK] ?: true,
            globalHttpsOnly = prefs[Keys.GLOBAL_HTTPS_ONLY] ?: true,
            globalBlockThirdPartyCookies = prefs[Keys.GLOBAL_BLOCK_3P_COOKIES] ?: true,
            globalClearOnExit = prefs[Keys.GLOBAL_CLEAR_ON_EXIT] ?: false,
            biometricGlobalEnabled = prefs[Keys.BIOMETRIC_GLOBAL] ?: false,
            pinCodeSha256 = prefs[Keys.PIN_SHA256] ?: "",
            totalBlockedRequests = prefs[Keys.TOTAL_BLOCKED_REQUESTS] ?: 0L
        )
    }

    suspend fun getSettingsOnce(): GlobalAppSettings = settingsFlow.first()

    suspend fun getActiveProfileId(): String {
        return context.dataStore.data.first()[Keys.ACTIVE_PROFILE_ID] ?: "default"
    }

    suspend fun setActiveProfileId(profileId: String) {
        context.dataStore.edit { it[Keys.ACTIVE_PROFILE_ID] = profileId }
    }

    suspend fun updateThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun updateDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun updateLanguage(languageCode: String) {
        context.dataStore.edit { it[Keys.LANGUAGE_CODE] = languageCode }
    }

    suspend fun updateGlobalAdBlock(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GLOBAL_AD_BLOCK] = enabled }
    }

    suspend fun updateGlobalHttpsOnly(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GLOBAL_HTTPS_ONLY] = enabled }
    }

    suspend fun updateGlobalBlockThirdPartyCookies(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GLOBAL_BLOCK_3P_COOKIES] = enabled }
    }

    suspend fun updateGlobalClearOnExit(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GLOBAL_CLEAR_ON_EXIT] = enabled }
    }

    suspend fun updateBiometricGlobal(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_GLOBAL] = enabled }
    }

    suspend fun setPinCodePlain(rawPin: String) {
        val hashed = if (rawPin.isBlank()) "" else sha256(rawPin.trim())
        context.dataStore.edit { it[Keys.PIN_SHA256] = hashed }
    }

    suspend fun verifyPinCode(candidatePin: String): Boolean {
        val expectedHash = context.dataStore.data.first()[Keys.PIN_SHA256].orEmpty()
        if (expectedHash.isBlank()) return true
        return sha256(candidatePin.trim()) == expectedHash
    }

    suspend fun incrementBlockedRequests(delta: Long = 1L) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.TOTAL_BLOCKED_REQUESTS] ?: 0L
            prefs[Keys.TOTAL_BLOCKED_REQUESTS] = current + delta
        }
    }

    suspend fun restoreGlobalSettings(settings: GlobalAppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = settings.themeMode.name
            prefs[Keys.DYNAMIC_COLOR] = settings.dynamicColorEnabled
            prefs[Keys.LANGUAGE_CODE] = settings.languageCode
            prefs[Keys.GLOBAL_AD_BLOCK] = settings.globalAdBlockEnabled
            prefs[Keys.GLOBAL_HTTPS_ONLY] = settings.globalHttpsOnly
            prefs[Keys.GLOBAL_BLOCK_3P_COOKIES] = settings.globalBlockThirdPartyCookies
            prefs[Keys.GLOBAL_CLEAR_ON_EXIT] = settings.globalClearOnExit
            prefs[Keys.BIOMETRIC_GLOBAL] = settings.biometricGlobalEnabled
            prefs[Keys.PIN_SHA256] = settings.pinCodeSha256
        }
    }

    companion object {
        fun sha256(input: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
