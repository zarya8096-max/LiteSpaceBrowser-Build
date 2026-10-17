package com.litespace.browser

import android.app.ActivityManager
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.ValueCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.litespace.browser.core.security.BiometricLockHelper
import com.litespace.browser.core.shortcut.ShortcutPinManager
import com.litespace.browser.data.repository.LiteAppRepository
import com.litespace.browser.domain.model.GlobalAppSettings
import com.litespace.browser.domain.model.LiteAppConfig
import com.litespace.browser.domain.model.OrientationMode
import com.litespace.browser.ui.screens.LiteAppBrowserScreen
import com.litespace.browser.ui.theme.LiteSpaceTheme
import kotlinx.coroutines.launch
import java.util.UUID

class LiteAppActivity : AppCompatActivity() {

    private var pendingFileChooserCallback: ValueCallback<Array<Uri>>? = null
    private var pendingPermissionCallback: ((Boolean) -> Unit)? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        val callback = pendingFileChooserCallback
        pendingFileChooserCallback = null
        if (uris.isEmpty()) {
            callback?.onReceiveValue(null)
        } else {
            callback?.onReceiveValue(uris.toTypedArray())
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants: Map<String, Boolean> ->
        val allGranted = grants.values.isNotEmpty() && grants.values.all { it }
        pendingPermissionCallback?.invoke(allGranted)
        pendingPermissionCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as LiteSpaceApplication
        val repository = app.repository

        val liteAppId = intent.getLongExtra(EXTRA_LITE_APP_ID, -1L)
        val overrideUrl = intent.getStringExtra(EXTRA_OVERRIDE_URL)
        val isIncognito = intent.getBooleanExtra(EXTRA_INCOGNITO, false)

        setContent {
            val globalSettings by app.preferencesRepository.settingsFlow.collectAsState(
                initial = GlobalAppSettings()
            )
            var loadedConfig by remember { mutableStateOf<LiteAppConfig?>(null) }
            var isUnlocked by remember { mutableStateOf(false) }
            var showPinFallback by remember { mutableStateOf(false) }
            var pinError by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(liteAppId) {
                val resolved = if (liteAppId > 0L) {
                    repository.recordAppLaunch(liteAppId)
                    repository.getLiteAppById(liteAppId)
                } else {
                    null
                } ?: LiteAppConfig(
                    id = -1L,
                    uuid = UUID.randomUUID().toString(),
                    name = if (isIncognito) "Incognito Session" else "Quick Session",
                    startUrl = LiteAppRepository.normalizeUrl(overrideUrl ?: "https://duckduckgo.com"),
                    profileId = if (isIncognito) "incognito" else "default"
                )

                loadedConfig = resolved
                applyTaskDescriptionAndOrientation(resolved)

                if (!resolved.biometricLockEnabled) {
                    isUnlocked = true
                } else {
                    BiometricLockHelper.showBiometricPrompt(
                        activity = this@LiteAppActivity,
                        appName = resolved.name,
                        onAuthenticated = { isUnlocked = true },
                        onFallbackToPin = { showPinFallback = true },
                        onCancelled = { finish() }
                    )
                }
            }

            val cfg = loadedConfig
            LiteSpaceTheme(
                themeMode = globalSettings.themeMode,
                dynamicColor = globalSettings.dynamicColorEnabled,
                languageCode = globalSettings.languageCode,
                customSeedHex = cfg?.themeColorHex
            ) {
                if (cfg == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (!isUnlocked) {
                    if (showPinFallback) {
                        var pinInput by remember { mutableStateOf("") }
                        AlertDialog(
                            onDismissRequest = { finish() },
                            title = { Text("${getString(R.string.biometric_prompt_title)}: ${cfg.name}") },
                            text = {
                                Column {
                                    OutlinedTextField(
                                        value = pinInput,
                                        onValueChange = {
                                            pinInput = it
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
                                            if (app.preferencesRepository.verifyPinCode(pinInput)) {
                                                isUnlocked = true
                                                showPinFallback = false
                                            } else {
                                                pinError = true
                                            }
                                        }
                                    }
                                ) {
                                    Text("Unlock")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { finish() }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else {
                    LiteAppBrowserScreen(
                        initialConfig = cfg,
                        overrideUrl = overrideUrl?.let { LiteAppRepository.normalizeUrl(it) },
                        isIncognito = isIncognito,
                        globalSettings = globalSettings,
                        repository = repository,
                        contentBlockerEngine = app.contentBlockerEngine,
                        profileIsolationManager = app.profileIsolationManager,
                        onShowFilePicker = { callback, acceptTypes ->
                            pendingFileChooserCallback?.onReceiveValue(null)
                            pendingFileChooserCallback = callback
                            val validMimeTypes = acceptTypes
                                .filter { it.contains("/") }
                                .ifEmpty { listOf("*/*") }
                                .toTypedArray()
                            runCatching {
                                filePickerLauncher.launch(validMimeTypes)
                                true
                            }.getOrElse {
                                pendingFileChooserCallback?.onReceiveValue(null)
                                pendingFileChooserCallback = null
                                false
                            }
                        },
                        onRequestPermissions = { permissions, resultCallback ->
                            pendingPermissionCallback = resultCallback
                            permissionLauncher.launch(permissions)
                        },
                        onToggleImmersiveSystemBars = { immersive ->
                            setImmersiveSystemBars(immersive)
                        },
                        onCloseLiteApp = { finishAndRemoveTask() }
                    )
                }
            }
        }
    }

    private fun applyTaskDescriptionAndOrientation(config: LiteAppConfig) {
        requestedOrientation = when (config.orientationMode) {
            OrientationMode.SYSTEM -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            OrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            OrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        val iconBitmap = ShortcutPinManager.renderLiteAppIconBitmap(
            context = this,
            iconText = config.iconText,
            colorHex = config.iconColorHex,
            customIconUri = config.customIconUri,
            sizePx = 144
        )
        val primaryColor = runCatching { Color.parseColor(config.themeColorHex) }.getOrDefault(Color.BLUE)

        @Suppress("DEPRECATION")
        val taskDesc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ActivityManager.TaskDescription(config.name, iconBitmap, primaryColor)
        } else {
            ActivityManager.TaskDescription(config.name, iconBitmap, primaryColor)
        }
        setTaskDescription(taskDesc)
    }

    private fun setImmersiveSystemBars(immersive: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (immersive) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    companion object {
        const val ACTION_LAUNCH_LITE_APP = "com.litespace.browser.action.LAUNCH_LITE_APP"
        const val EXTRA_LITE_APP_ID = "extra_lite_app_id"
        const val EXTRA_LITE_APP_UUID = "extra_lite_app_uuid"
        const val EXTRA_OVERRIDE_URL = "extra_override_url"
        const val EXTRA_INCOGNITO = "extra_incognito"
    }
}
