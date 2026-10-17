package com.litespace.browser.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.litespace.browser.core.webview.ContentBlockerEngine
import com.litespace.browser.data.repository.LiteAppRepository
import com.litespace.browser.domain.model.AppThemeMode
import com.litespace.browser.domain.model.CatalogTemplate
import com.litespace.browser.domain.model.ContentFilterRule
import com.litespace.browser.domain.model.GlobalAppSettings
import com.litespace.browser.domain.model.LiteAppCategory
import com.litespace.browser.domain.model.LiteAppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(
    private val repository: LiteAppRepository,
    private val contentBlockerEngine: ContentBlockerEngine
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<LiteAppCategory?>(null)
    val selectedCategory: StateFlow<LiteAppCategory?> = _selectedCategory.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    val globalSettings: StateFlow<GlobalAppSettings> = repository.preferencesRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GlobalAppSettings())

    val allLiteApps: StateFlow<List<LiteAppConfig>> = repository.allLiteAppsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filteredLiteApps: StateFlow<List<LiteAppConfig>> = combine(
        allLiteApps,
        _searchQuery,
        _selectedCategory
    ) { apps, query, category ->
        val trimmed = query.trim().lowercase()
        apps.filter { app ->
            val matchesCat = category == null || app.category == category
            val matchesSearch = trimmed.isEmpty() ||
                app.name.lowercase().contains(trimmed) ||
                app.startUrl.lowercase().contains(trimmed) ||
                app.profileId.lowercase().contains(trimmed)
            matchesCat && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filterRules: StateFlow<List<ContentFilterRule>> = repository.allFilterRulesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            val rules = repository.getEnabledFilterRules()
            contentBlockerEngine.ensureInitialized(rules)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: LiteAppCategory?) {
        _selectedCategory.value = category
    }

    fun clearStatusMessage() {
        _statusBannerMessage.value = null
    }

    fun postStatusMessage(msg: String) {
        _statusBannerMessage.value = msg
    }

    fun saveLiteApp(config: LiteAppConfig, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val savedId = repository.saveLiteApp(config)
            _statusBannerMessage.value = "Saved Lite App: ${config.name}"
            onSaved(savedId)
        }
    }

    fun createFromTemplate(template: CatalogTemplate, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val newConfig = LiteAppConfig(
                uuid = UUID.randomUUID().toString(),
                name = template.name,
                startUrl = template.startUrl,
                category = template.category,
                iconText = template.iconText,
                iconColorHex = template.iconColorHex,
                themeColorHex = template.iconColorHex,
                profileId = "profile_${template.name.lowercase().replace(" ", "_")}",
                feedMonitorUrl = template.feedUrl,
                feedCheckIntervalMinutes = if (template.feedUrl.isNotBlank()) 60 else 0
            )
            val id = repository.saveLiteApp(newConfig)
            _statusBannerMessage.value = "Added ${template.name} to your Lite Apps"
            onCreated(id)
        }
    }

    fun toggleFavorite(app: LiteAppConfig) {
        viewModelScope.launch {
            repository.saveLiteApp(app.copy(isFavorite = !app.isFavorite))
        }
    }

    fun deleteLiteApp(appId: Long, appName: String) {
        viewModelScope.launch {
            repository.deleteLiteApp(appId)
            _statusBannerMessage.value = "Deleted $appName"
        }
    }

    fun addFilterRule(pattern: String, isException: Boolean) {
        viewModelScope.launch {
            val id = repository.addFilterRule(pattern, isException)
            if (id > 0) {
                contentBlockerEngine.reloadCustomRules(repository.getEnabledFilterRules())
                _statusBannerMessage.value = "Filter rule added: $pattern"
            }
        }
    }

    fun toggleFilterRule(rule: ContentFilterRule) {
        viewModelScope.launch {
            repository.toggleFilterRule(rule.id, !rule.isEnabled)
            contentBlockerEngine.reloadCustomRules(repository.getEnabledFilterRules())
        }
    }

    fun deleteFilterRule(ruleId: Long) {
        viewModelScope.launch {
            repository.deleteFilterRule(ruleId)
            contentBlockerEngine.reloadCustomRules(repository.getEnabledFilterRules())
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { repository.preferencesRepository.updateThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { repository.preferencesRepository.updateDynamicColor(enabled) }
    }

    fun setLanguageCode(lang: String) {
        viewModelScope.launch { repository.preferencesRepository.updateLanguage(lang) }
    }

    fun setGlobalAdBlock(enabled: Boolean) {
        viewModelScope.launch { repository.preferencesRepository.updateGlobalAdBlock(enabled) }
    }

    fun setGlobalHttpsOnly(enabled: Boolean) {
        viewModelScope.launch { repository.preferencesRepository.updateGlobalHttpsOnly(enabled) }
    }

    fun setGlobalBlockThirdPartyCookies(enabled: Boolean) {
        viewModelScope.launch { repository.preferencesRepository.updateGlobalBlockThirdPartyCookies(enabled) }
    }

    fun setGlobalClearOnExit(enabled: Boolean) {
        viewModelScope.launch { repository.preferencesRepository.updateGlobalClearOnExit(enabled) }
    }

    fun setBiometricGlobal(enabled: Boolean) {
        viewModelScope.launch { repository.preferencesRepository.updateBiometricGlobal(enabled) }
    }

    fun updatePinCode(rawPin: String) {
        viewModelScope.launch {
            repository.preferencesRepository.setPinCodePlain(rawPin)
            _statusBannerMessage.value = if (rawPin.isBlank()) "Security PIN cleared" else "Security PIN updated"
        }
    }

    suspend fun verifyPin(candidatePin: String): Boolean {
        return repository.preferencesRepository.verifyPinCode(candidatePin)
    }

    suspend fun exportBackupJson(): String {
        return repository.exportBackupJson()
    }

    fun importBackupJson(rawJson: String) {
        viewModelScope.launch {
            val result = repository.importBackupJson(rawJson)
            result.onSuccess { count ->
                contentBlockerEngine.reloadCustomRules(repository.getEnabledFilterRules())
                _statusBannerMessage.value = "Restored $count Lite Apps and settings successfully"
            }.onFailure { err ->
                _statusBannerMessage.value = "Backup import failed: ${err.localizedMessage}"
            }
        }
    }

    class Factory(
        private val repository: LiteAppRepository,
        private val contentBlockerEngine: ContentBlockerEngine
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository, contentBlockerEngine) as T
        }
    }
}
