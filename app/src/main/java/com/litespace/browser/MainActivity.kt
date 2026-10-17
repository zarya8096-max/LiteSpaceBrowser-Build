package com.litespace.browser

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import com.litespace.browser.core.security.BiometricLockHelper
import com.litespace.browser.core.shortcut.ShortcutPinManager
import com.litespace.browser.domain.model.LiteAppConfig
import com.litespace.browser.ui.screens.BackupRestoreScreen
import com.litespace.browser.ui.screens.DashboardScreen
import com.litespace.browser.ui.screens.GlobalSettingsScreen
import com.litespace.browser.ui.screens.LiteAppEditorScreen
import com.litespace.browser.ui.screens.PrivacyDashboardScreen
import com.litespace.browser.ui.theme.LiteSpaceTheme
import com.litespace.browser.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.util.UUID

private enum class MainTab {
    DASHBOARD, PRIVACY, BACKUP, SETTINGS
}

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as LiteSpaceApplication
        MainViewModel.Factory(app.repository, app.contentBlockerEngine)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestPostNotificationsIfNeeded()

        val sharedUrl = extractSharedUrl(intent)

        setContent {
            val settings by viewModel.globalSettings.collectAsState()
            val liteApps by viewModel.filteredLiteApps.collectAsState()
            val searchQuery by viewModel.searchQuery.collectAsState()
            val selectedCategory by viewModel.selectedCategory.collectAsState()
            val filterRules by viewModel.filterRules.collectAsState()
            val statusMessage by viewModel.statusBannerMessage.collectAsState()

            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()

            var activeTab by remember { mutableStateOf(MainTab.DASHBOARD) }
            var editingConfig by remember {
                mutableStateOf<LiteAppConfig?>(
                    if (!sharedUrl.isNullOrBlank()) {
                        LiteAppConfig(
                            uuid = UUID.randomUUID().toString(),
                            name = "Shared Web App",
                            startUrl = sharedUrl
                        )
                    } else null
                )
            }

            var isAppUnlocked by remember { mutableStateOf(false) }
            var showPinDialog by remember { mutableStateOf(false) }
            var pinError by remember { mutableStateOf(false) }

            LaunchedEffect(settings.biometricGlobalEnabled) {
                if (!settings.biometricGlobalEnabled) {
                    isAppUnlocked = true
                } else if (!isAppUnlocked) {
                    BiometricLockHelper.showBiometricPrompt(
                        activity = this@MainActivity,
                        appName = getString(R.string.app_name),
                        onAuthenticated = { isAppUnlocked = true },
                        onFallbackToPin = { showPinDialog = true },
                        onCancelled = { showPinDialog = true }
                    )
                }
            }

            LaunchedEffect(statusMessage) {
                val msg = statusMessage
                if (!msg.isNullOrBlank()) {
                    snackbarHostState.showSnackbar(msg)
                    viewModel.clearStatusMessage()
                }
            }

            LiteSpaceTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColorEnabled,
                languageCode = settings.languageCode
            ) {
                val isPersian = settings.languageCode.startsWith("fa")

                if (showPinDialog && !isAppUnlocked) {
                    var pinCandidate by remember { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text(stringResource(R.string.pin_dialog_title)) },
                        text = {
                            Column {
                                OutlinedTextField(
                                    value = pinCandidate,
                                    onValueChange = {
                                        pinCandidate = it
                                        pinError = false
                                    },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (pinError) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(stringResource(R.string.pin_incorrect))
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    scope.launch {
                                        if (viewModel.verifyPin(pinCandidate)) {
                                            isAppUnlocked = true
                                            showPinDialog = false
                                        } else {
                                            pinError = true
                                        }
                                    }
                                }
                            ) {
                                Text("Unlock")
                            }
                        }
                    )
                }

                val editorTarget = editingConfig
                if (editorTarget != null) {
                    LiteAppEditorScreen(
                        initialConfig = editorTarget,
                        isPersian = isPersian,
                        onSave = { updated ->
                            viewModel.saveLiteApp(updated) {
                                editingConfig = null
                            }
                        },
                        onCancel = { editingConfig = null }
                    )
                } else {
                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = activeTab == MainTab.DASHBOARD,
                                    onClick = { activeTab = MainTab.DASHBOARD },
                                    icon = { Icon(Icons.Default.Apps, contentDescription = null) },
                                    label = { Text(stringResource(R.string.nav_dashboard)) }
                                )
                                NavigationBarItem(
                                    selected = activeTab == MainTab.PRIVACY,
                                    onClick = { activeTab = MainTab.PRIVACY },
                                    icon = { Icon(Icons.Default.Security, contentDescription = null) },
                                    label = { Text(stringResource(R.string.nav_privacy)) }
                                )
                                NavigationBarItem(
                                    selected = activeTab == MainTab.BACKUP,
                                    onClick = { activeTab = MainTab.BACKUP },
                                    icon = { Icon(Icons.Default.CloudSync, contentDescription = null) },
                                    label = { Text(stringResource(R.string.nav_backup)) }
                                )
                                NavigationBarItem(
                                    selected = activeTab == MainTab.SETTINGS,
                                    onClick = { activeTab = MainTab.SETTINGS },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                    label = { Text(stringResource(R.string.nav_settings)) }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = innerPadding.calculateBottomPadding())
                        ) {
                            when (activeTab) {
                                MainTab.DASHBOARD -> DashboardScreen(
                                    liteApps = liteApps,
                                    searchQuery = searchQuery,
                                    selectedCategory = selectedCategory,
                                    languageCode = settings.languageCode,
                                    totalBlockedCount = settings.totalBlockedRequests,
                                    onSearchQueryChange = viewModel::updateSearchQuery,
                                    onSelectCategory = viewModel::selectCategory,
                                    onToggleLanguage = {
                                        val nextLang = if (isPersian) "en" else "fa"
                                        viewModel.setLanguageCode(nextLang)
                                        AppCompatDelegate.setApplicationLocales(
                                            LocaleListCompat.forLanguageTags(nextLang)
                                        )
                                    },
                                    onCreateNewApp = { initialUrl ->
                                        editingConfig = LiteAppConfig(
                                            uuid = UUID.randomUUID().toString(),
                                            name = "",
                                            startUrl = initialUrl.ifBlank { "https://" }
                                        )
                                    },
                                    onEditApp = { app -> editingConfig = app },
                                    onLaunchApp = { app, incognito ->
                                        launchStandaloneLiteApp(app.id, null, incognito)
                                    },
                                    onQuickIncognitoUrl = { rawQuery ->
                                        launchStandaloneLiteApp(-1L, rawQuery, true)
                                    },
                                    onPinShortcut = { app ->
                                        val ok = ShortcutPinManager.pinLiteAppToHomeScreen(
                                            this@MainActivity,
                                            app
                                        )
                                        viewModel.postStatusMessage(
                                            getString(
                                                if (ok) R.string.pinned_shortcut_success
                                                else R.string.pinned_shortcut_unsupported
                                            )
                                        )
                                    },
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onDeleteApp = { app -> viewModel.deleteLiteApp(app.id, app.name) },
                                    onAddTemplate = { tpl -> viewModel.createFromTemplate(tpl) }
                                )

                                MainTab.PRIVACY -> PrivacyDashboardScreen(
                                    settings = settings,
                                    filterRules = filterRules,
                                    isPersian = isPersian,
                                    onToggleGlobalAdBlock = viewModel::setGlobalAdBlock,
                                    onToggleGlobalHttpsOnly = viewModel::setGlobalHttpsOnly,
                                    onToggleGlobalThirdPartyCookies = viewModel::setGlobalBlockThirdPartyCookies,
                                    onToggleGlobalClearOnExit = viewModel::setGlobalClearOnExit,
                                    onAddFilterRule = viewModel::addFilterRule,
                                    onToggleRule = viewModel::toggleFilterRule,
                                    onDeleteRule = viewModel::deleteFilterRule
                                )

                                MainTab.BACKUP -> BackupRestoreScreen(
                                    isPersian = isPersian,
                                    onProduceBackupJson = viewModel::exportBackupJson,
                                    onImportBackupJson = viewModel::importBackupJson,
                                    onStatusMessage = viewModel::postStatusMessage
                                )

                                MainTab.SETTINGS -> GlobalSettingsScreen(
                                    settings = settings,
                                    onSelectThemeMode = viewModel::setThemeMode,
                                    onToggleDynamicColor = viewModel::setDynamicColor,
                                    onSelectLanguage = { nextLang ->
                                        viewModel.setLanguageCode(nextLang)
                                        AppCompatDelegate.setApplicationLocales(
                                            LocaleListCompat.forLanguageTags(nextLang)
                                        )
                                    },
                                    onToggleBiometricGlobal = viewModel::setBiometricGlobal,
                                    onUpdatePinCode = viewModel::updatePinCode
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun launchStandaloneLiteApp(liteAppId: Long, overrideUrl: String?, incognito: Boolean) {
        val launchIntent = Intent(this, LiteAppActivity::class.java).apply {
            action = LiteAppActivity.ACTION_LAUNCH_LITE_APP
            putExtra(LiteAppActivity.EXTRA_LITE_APP_ID, liteAppId)
            if (!overrideUrl.isNullOrBlank()) {
                putExtra(LiteAppActivity.EXTRA_OVERRIDE_URL, overrideUrl)
            }
            putExtra(LiteAppActivity.EXTRA_INCOGNITO, incognito)
            addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }
        startActivity(launchIntent)
    }

    private fun extractSharedUrl(intent: Intent?): String? {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            return intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
        }
        return null
    }
}
