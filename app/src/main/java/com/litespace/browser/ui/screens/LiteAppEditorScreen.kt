package com.litespace.browser.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.litespace.browser.R
import com.litespace.browser.data.repository.LiteAppRepository
import com.litespace.browser.domain.model.CatalogTemplates
import com.litespace.browser.domain.model.DisplayMode
import com.litespace.browser.domain.model.LiteAppCategory
import com.litespace.browser.domain.model.LiteAppConfig
import com.litespace.browser.domain.model.OrientationMode
import com.litespace.browser.domain.model.PermissionSetting
import com.litespace.browser.domain.model.PopupPolicy
import com.litespace.browser.domain.model.UserAgentPreset
import com.litespace.browser.ui.theme.FeatureMicroChip
import com.litespace.browser.ui.theme.LiteAppSquircleIcon
import com.litespace.browser.ui.theme.parseHexColorSafely

private val PresetColors = listOf(
    "#0B57D0", "#00897B", "#2E7D32", "#E6123D",
    "#6A1B9A", "#E65100", "#0088CC", "#283593",
    "#AD1457", "#4527A0", "#0277BD", "#37474F"
)

private val PresetProfiles = listOf("default", "work", "personal", "banking", "social_sandbox", "shopping_isolated")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LiteAppEditorScreen(
    initialConfig: LiteAppConfig,
    isPersian: Boolean,
    onSave: (LiteAppConfig) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    var name by remember(initialConfig.uuid) { mutableStateOf(initialConfig.name) }
    var startUrl by remember(initialConfig.uuid) {
        mutableStateOf(
            if (initialConfig.startUrl == "https://" || initialConfig.startUrl == "http://") ""
            else initialConfig.startUrl
        )
    }
    var category by remember(initialConfig.uuid) { mutableStateOf(initialConfig.category) }
    var iconText by remember(initialConfig.uuid) { mutableStateOf(initialConfig.iconText) }
    var userEditedMonogram by remember(initialConfig.uuid) { mutableStateOf(initialConfig.id != 0L) }
    var iconColorHex by remember(initialConfig.uuid) { mutableStateOf(initialConfig.iconColorHex) }
    var customIconUri by remember(initialConfig.uuid) { mutableStateOf(initialConfig.customIconUri) }
    var profileId by remember(initialConfig.uuid) { mutableStateOf(initialConfig.profileId) }

    var displayMode by remember(initialConfig.uuid) { mutableStateOf(initialConfig.displayMode) }
    var orientationMode by remember(initialConfig.uuid) { mutableStateOf(initialConfig.orientationMode) }
    var forceDarkWeb by remember(initialConfig.uuid) { mutableStateOf(initialConfig.forceDarkWeb) }
    var pullToRefresh by remember(initialConfig.uuid) { mutableStateOf(initialConfig.pullToRefreshEnabled) }
    var navGestures by remember(initialConfig.uuid) { mutableStateOf(initialConfig.navigationGesturesEnabled) }
    var showBottomBar by remember(initialConfig.uuid) { mutableStateOf(initialConfig.showBottomBar) }

    var jsEnabled by remember(initialConfig.uuid) { mutableStateOf(initialConfig.javaScriptEnabled) }
    var domStorageEnabled by remember(initialConfig.uuid) { mutableStateOf(initialConfig.domStorageEnabled) }
    var desktopMode by remember(initialConfig.uuid) { mutableStateOf(initialConfig.desktopMode) }
    var userAgentPreset by remember(initialConfig.uuid) { mutableStateOf(initialConfig.userAgentPreset) }
    var customUserAgent by remember(initialConfig.uuid) { mutableStateOf(initialConfig.customUserAgent) }
    var textZoom by remember(initialConfig.uuid) { mutableFloatStateOf(initialConfig.textZoomPercent.toFloat()) }
    var pageZoom by remember(initialConfig.uuid) { mutableFloatStateOf(initialConfig.defaultPageZoomPercent.toFloat()) }
    var enableZoomControls by remember(initialConfig.uuid) { mutableStateOf(initialConfig.enableZoomControls) }

    var customCss by remember(initialConfig.uuid) { mutableStateOf(initialConfig.customCss) }
    var customJs by remember(initialConfig.uuid) { mutableStateOf(initialConfig.customJs) }
    var injectAtStart by remember(initialConfig.uuid) { mutableStateOf(initialConfig.injectAtDocumentStart) }

    var adBlockEnabled by remember(initialConfig.uuid) { mutableStateOf(initialConfig.adBlockEnabled) }
    var blockThirdPartyCookies by remember(initialConfig.uuid) { mutableStateOf(initialConfig.blockThirdPartyCookies) }
    var acceptCookies by remember(initialConfig.uuid) { mutableStateOf(initialConfig.acceptCookies) }
    var httpsUpgrade by remember(initialConfig.uuid) { mutableStateOf(initialConfig.httpsUpgradeEnabled) }
    var clearCacheOnExit by remember(initialConfig.uuid) { mutableStateOf(initialConfig.clearCacheOnExit) }
    var clearCookiesOnExit by remember(initialConfig.uuid) { mutableStateOf(initialConfig.clearCookiesOnExit) }
    var clearStorageOnExit by remember(initialConfig.uuid) { mutableStateOf(initialConfig.clearStorageOnExit) }
    var openExternalInBrowser by remember(initialConfig.uuid) { mutableStateOf(initialConfig.openExternalLinksInBrowser) }
    var popupPolicy by remember(initialConfig.uuid) { mutableStateOf(initialConfig.popupPolicy) }
    var biometricLock by remember(initialConfig.uuid) { mutableStateOf(initialConfig.biometricLockEnabled) }

    var allowGeolocation by remember(initialConfig.uuid) { mutableStateOf(initialConfig.allowGeolocation) }
    var allowCamera by remember(initialConfig.uuid) { mutableStateOf(initialConfig.allowCamera) }
    var allowMicrophone by remember(initialConfig.uuid) { mutableStateOf(initialConfig.allowMicrophone) }
    var allowWebNotifications by remember(initialConfig.uuid) { mutableStateOf(initialConfig.allowWebNotifications) }

    var feedMonitorUrl by remember(initialConfig.uuid) { mutableStateOf(initialConfig.feedMonitorUrl) }
    var feedIntervalMinutes by remember(initialConfig.uuid) { mutableIntStateOf(initialConfig.feedCheckIntervalMinutes) }

    var activeSectionIndex by remember { mutableIntStateOf(0) }
    val sectionTitles = remember(isPersian) {
        if (isPersian) {
            listOf("۱. هویت و آیکون", "۲. نمایش و پوسته", "۳. موتور وب‌ویو", "۴. اسکریپت CSS/JS", "۵. حریم خصوصی", "۶. اعلان و خوراک")
        } else {
            listOf("1. Identity & Icon", "2. Display & Theme", "3. WebView Engine", "4. Custom CSS/JS", "5. Privacy & Lock", "6. Notifications")
        }
    }

    val iconPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            customIconUri = uri.toString()
        }
    }

    val commitSave = {
        val rawUrl = startUrl.trim()
        val resolvedUrl = if (rawUrl.isNotBlank()) {
            LiteAppRepository.normalizeUrl(rawUrl)
        } else if (name.trim().isNotBlank()) {
            LiteAppRepository.normalizeUrl(name.trim())
        } else {
            "https://duckduckgo.com"
        }
        val resolvedName = name.trim().ifBlank {
            LiteAppRepository.extractHostLabel(resolvedUrl)
        }
        val resolvedIconText = iconText.trim().take(2).uppercase().ifBlank {
            resolvedName.filter { it.isLetterOrDigit() }.take(2).uppercase().ifBlank { "LA" }
        }
        val updated = initialConfig.copy(
            name = resolvedName,
            startUrl = resolvedUrl,
            category = category,
            iconText = resolvedIconText,
            iconColorHex = iconColorHex,
            customIconUri = customIconUri,
            themeColorHex = iconColorHex,
            profileId = profileId.trim().ifBlank { "default" },
            displayMode = displayMode,
            orientationMode = orientationMode,
            forceDarkWeb = forceDarkWeb,
            pullToRefreshEnabled = pullToRefresh,
            navigationGesturesEnabled = navGestures,
            showBottomBar = showBottomBar,
            javaScriptEnabled = jsEnabled,
            domStorageEnabled = domStorageEnabled,
            desktopMode = desktopMode,
            userAgentPreset = userAgentPreset,
            customUserAgent = customUserAgent.trim(),
            textZoomPercent = textZoom.toInt(),
            defaultPageZoomPercent = pageZoom.toInt(),
            enableZoomControls = enableZoomControls,
            customCss = customCss,
            customJs = customJs,
            injectAtDocumentStart = injectAtStart,
            adBlockEnabled = adBlockEnabled,
            blockThirdPartyCookies = blockThirdPartyCookies,
            acceptCookies = acceptCookies,
            httpsUpgradeEnabled = httpsUpgrade,
            clearCacheOnExit = clearCacheOnExit,
            clearCookiesOnExit = clearCookiesOnExit,
            clearStorageOnExit = clearStorageOnExit,
            openExternalLinksInBrowser = openExternalInBrowser,
            popupPolicy = popupPolicy,
            biometricLockEnabled = biometricLock,
            allowGeolocation = allowGeolocation,
            allowCamera = allowCamera,
            allowMicrophone = allowMicrophone,
            allowWebNotifications = allowWebNotifications,
            feedMonitorUrl = feedMonitorUrl.trim(),
            feedCheckIntervalMinutes = feedIntervalMinutes
        )
        onSave(updated)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (initialConfig.id == 0L) {
                            stringResource(R.string.editor_create_title)
                        } else {
                            stringResource(R.string.editor_edit_title)
                        },
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = commitSave,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.save_lite_app), fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Interactive Phone Header & Squircle Preview Card
            val previewBrandColor = parseHexColorSafely(iconColorHex)
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Simulated Mini Browser Top Bar Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(previewBrandColor, previewBrandColor.copy(alpha = 0.78f))
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isPersian) "پیش‌نمایش نوار وضعیت اختصاصی لایت‌اپ" else "Live Standalone Window TopBar Preview",
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = iconColorHex.uppercase(),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiteAppSquircleIcon(
                            iconText = iconText.ifBlank { name.take(2).uppercase() },
                            colorHex = iconColorHex,
                            customIconUri = customIconUri,
                            size = 62.dp
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name.ifBlank { if (isPersian) "نام لایت‌اپ جدید" else "New Lite App" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = startUrl.ifBlank { "https://example.com" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FeatureMicroChip(label = profileId)
                                FeatureMicroChip(
                                    label = if (isPersian) displayMode.labelFa else displayMode.labelEn,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // 1-Tap Quick-Fill Presets (for instant app creation without typing)
            if (initialConfig.id == 0L) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isPersian) "پر کردن سریع با یک لمس (الگوهای محبوب):" else "1-Tap Quick Auto-Fill Presets:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(CatalogTemplates.builtInTemplates, key = { it.name }) { tpl ->
                            AssistChip(
                                onClick = {
                                    name = tpl.name
                                    startUrl = tpl.startUrl
                                    category = tpl.category
                                    iconText = tpl.iconText
                                    iconColorHex = tpl.iconColorHex
                                    feedMonitorUrl = tpl.feedUrl
                                    userEditedMonogram = true
                                },
                                label = { Text(tpl.name, fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }
                }
            }

            // Section Selector Tabs
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(sectionTitles) { index, label ->
                    FilterChip(
                        selected = activeSectionIndex == index,
                        onClick = { activeSectionIndex = index },
                        label = { Text(label, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            AnimatedContent(targetState = activeSectionIndex, label = "editor_section_anim") { sectionIdx ->
                when (sectionIdx) {
                    0 -> EditorSectionCard(title = sectionTitles[0]) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { newName ->
                                name = newName
                                if (!userEditedMonogram) {
                                    val letters = newName.trim().filter { it.isLetterOrDigit() }.take(2).uppercase()
                                    iconText = letters.ifBlank { "LA" }
                                }
                            },
                            label = { Text(stringResource(R.string.field_app_name)) },
                            placeholder = { Text(if (isPersian) "مثلاً: دیجی‌کالا، دیوار، گیت‌هاب" else "e.g. GitHub, Wikipedia, Digikala") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = startUrl,
                            onValueChange = { newUrl ->
                                startUrl = newUrl
                                if (name.isBlank() && newUrl.contains(".")) {
                                    val hostGuess = LiteAppRepository.extractHostLabel(
                                        LiteAppRepository.normalizeUrl(newUrl)
                                    )
                                    if (hostGuess != "Lite App") {
                                        name = hostGuess
                                        if (!userEditedMonogram) {
                                            iconText = hostGuess.take(2).uppercase()
                                        }
                                    }
                                }
                            },
                            label = { Text(stringResource(R.string.field_app_url)) },
                            placeholder = { Text("https://digikala.com") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = iconText,
                                onValueChange = {
                                    userEditedMonogram = true
                                    iconText = it.take(2).uppercase()
                                },
                                label = { Text(if (isPersian) "حروف آیکون (۲ حرف)" else "Icon Monogram (2 chars)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = {
                                    runCatching { iconPickerLauncher.launch(arrayOf("image/*")) }
                                }
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(if (isPersian) "آیکون گالری" else "Custom Icon")
                            }
                            if (!customIconUri.isNullOrBlank()) {
                                OutlinedButton(onClick = { customIconUri = null }) {
                                    Text(if (isPersian) "حذف" else "Reset")
                                }
                            }
                        }

                        Text(
                            text = if (isPersian) "رنگ برند و نوار ابزار:" else "Brand & Status Bar Color:",
                            style = MaterialTheme.typography.labelLarge
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PresetColors.forEach { hex ->
                                val selected = iconColorHex.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(parseHexColorSafely(hex))
                                        .border(
                                            width = if (selected) 3.dp else 1.dp,
                                            color = if (selected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.25f),
                                            shape = CircleShape
                                        )
                                        .clickable { iconColorHex = hex }
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.field_category),
                            style = MaterialTheme.typography.labelLarge
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LiteAppCategory.entries.forEach { cat ->
                                FilterChip(
                                    selected = category == cat,
                                    onClick = { category = cat },
                                    label = { Text(if (isPersian) cat.labelFa else cat.labelEn) }
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.field_profile_group),
                            style = MaterialTheme.typography.labelLarge
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PresetProfiles.forEach { prof ->
                                FilterChip(
                                    selected = profileId == prof,
                                    onClick = { profileId = prof },
                                    label = { Text(prof) }
                                )
                            }
                        }
                    }

                    1 -> EditorSectionCard(title = stringResource(R.string.section_display_mode)) {
                        Text(
                            text = if (isPersian) "حالت پنجره و نمایش:" else "Window Display Mode:",
                            style = MaterialTheme.typography.labelLarge
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DisplayMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = displayMode == mode,
                                    onClick = { displayMode = mode },
                                    label = { Text(if (isPersian) mode.labelFa else mode.labelEn) }
                                )
                            }
                        }

                        Text(
                            text = if (isPersian) "جهت چرخش صفحه:" else "Screen Orientation:",
                            style = MaterialTheme.typography.labelLarge
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OrientationMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = orientationMode == mode,
                                    onClick = { orientationMode = mode },
                                    label = { Text(if (isPersian) mode.labelFa else mode.labelEn) }
                                )
                            }
                        }

                        SettingSwitchRow(
                            title = if (isPersian) "حالت تیره هوشمند برای صفحات وب" else "Force Dark Mode on Web Pages",
                            checked = forceDarkWeb,
                            onCheckedChange = { forceDarkWeb = it }
                        )
                        SettingSwitchRow(
                            title = if (isPersian) "کشیدن صفحه به پایین برای بارگذاری مجدد (Pull-to-Refresh)" else "Pull-to-Refresh Gesture",
                            checked = pullToRefresh,
                            onCheckedChange = { pullToRefresh = it }
                        )
                        SettingSwitchRow(
                            title = if (isPersian) "ژست‌های حرکتی ناوبری" else "Edge Swipe Navigation Gestures",
                            checked = navGestures,
                            onCheckedChange = { navGestures = it }
                        )
                        SettingSwitchRow(
                            title = if (isPersian) "نمایش نوار ابزار پایین" else "Show Bottom Quick-Action Bar",
                            checked = showBottomBar,
                            onCheckedChange = { showBottomBar = it }
                        )
                    }

                    2 -> EditorSectionCard(title = stringResource(R.string.section_webview_engine)) {
                        SettingSwitchRow(
                            title = if (isPersian) "اجرای جاوااسکریپت (JavaScript)" else "Enable JavaScript",
                            checked = jsEnabled,
                            onCheckedChange = { jsEnabled = it }
                        )
                        SettingSwitchRow(
                            title = if (isPersian) "حافظه DOM و LocalStorage" else "Enable DOM Storage & IndexedDB",
                            checked = domStorageEnabled,
                            onCheckedChange = { domStorageEnabled = it }
                        )
                        SettingSwitchRow(
                            title = if (isPersian) "درخواست نسخه دسکتاپ سایت" else "Request Desktop Site by Default",
                            checked = desktopMode,
                            onCheckedChange = { desktopMode = it }
                        )
                        SettingSwitchRow(
                            title = if (isPersian) "قابلیت بزرگ‌نمایی دو انگشتی (Pinch Zoom)" else "Allow Page Pinch-to-Zoom",
                            checked = enableZoomControls,
                            onCheckedChange = { enableZoomControls = it }
                        )

                        Text(
                            text = if (isPersian) "اندازه فونت متن: ${textZoom.toInt()}%" else "Font Text Scaling: ${textZoom.toInt()}%",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Slider(
                            value = textZoom,
                            onValueChange = { textZoom = it },
                            valueRange = 60f..200f,
                            steps = 13
                        )

                        Text(
                            text = if (isPersian) "بزرگ‌نمایی اولیه صفحه: ${pageZoom.toInt()}%" else "Initial Page Zoom: ${pageZoom.toInt()}%",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Slider(
                            value = pageZoom,
                            onValueChange = { pageZoom = it },
                            valueRange = 50f..200f,
                            steps = 14
                        )

                        Text(
                            text = "User-Agent Preset:",
                            style = MaterialTheme.typography.labelLarge
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            UserAgentPreset.entries.forEach { preset ->
                                FilterChip(
                                    selected = userAgentPreset == preset,
                                    onClick = { userAgentPreset = preset },
                                    label = { Text(preset.labelEn) }
                                )
                            }
                        }
                        if (userAgentPreset == UserAgentPreset.CUSTOM) {
                            OutlinedTextField(
                                value = customUserAgent,
                                onValueChange = { customUserAgent = it },
                                label = { Text("Custom User-Agent String") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    3 -> EditorSectionCard(title = stringResource(R.string.section_scripts)) {
                        Text(
                            text = if (isPersian) "افزونه‌های آماده (با یک لمس اضافه کنید):" else "1-Tap Built-in Extensions:",
                            style = MaterialTheme.typography.labelLarge
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CatalogTemplates.builtInExtensions.forEach { ext ->
                                FilterChip(
                                    selected = (ext.cssSnippet.isNotBlank() && customCss.contains(ext.cssSnippet)) ||
                                        (ext.jsSnippet.isNotBlank() && customJs.contains(ext.jsSnippet)),
                                    onClick = {
                                        if (ext.cssSnippet.isNotBlank() && !customCss.contains(ext.cssSnippet)) {
                                            customCss = (customCss + "\n" + ext.cssSnippet).trim()
                                        }
                                        if (ext.jsSnippet.isNotBlank() && !customJs.contains(ext.jsSnippet)) {
                                            customJs = (customJs + "\n" + ext.jsSnippet).trim()
                                        }
                                    },
                                    label = { Text(if (isPersian) ext.titleFa else ext.titleEn) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = customCss,
                            onValueChange = { customCss = it },
                            label = { Text("Custom CSS Stylesheet Injection (UTF-8 supported)") },
                            minLines = 4,
                            maxLines = 8,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = customJs,
                            onValueChange = { customJs = it },
                            label = { Text("Custom JavaScript UserScript Injection (UTF-8 supported)") },
                            minLines = 4,
                            maxLines = 8,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.fillMaxWidth()
                        )
                        SettingSwitchRow(
                            title = if (isPersian) "تزریق اسکریپت در ابتدای بارگذاری سند (document_start)" else "Inject UserScript at Document Start (onPageStarted)",
                            checked = injectAtStart,
                            onCheckedChange = { injectAtStart = it }
                        )
                    }

                    4 -> EditorSectionCard(title = stringResource(R.string.section_privacy_security)) {
                        SettingSwitchRow(
                            title = if (isPersian) "مسدودسازی تبلیغات و ردیاب‌ها در این لایت‌اپ" else "Block Ads & Trackers in this Lite App",
                            checked = adBlockEnabled,
                            onCheckedChange = { adBlockEnabled = it }
                        )
                        SettingSwitchRow(
                            title = if (isPersian) "مسدودسازی کوکی‌های شخص ثالث (Third-Party Cookies)" else "Block Third-Party Cookies",
                            checked = blockThirdPartyCookies,
                            onCheckedChange = { blockThirdPartyCookies = it }
                        )
                        SettingSwitchRow(
                            title = if (isPersian) "پذیرش کوکی‌های اصلی سایت (First-Party Cookies)" else "Accept First-Party Site Cookies",
                            checked = acceptCookies,
                            onCheckedChange = { acceptCookies = it }
                        )
                        SettingSwitchRow(
                            title = if (isPersian) "ارتقای خودکار ارتباط به HTTPS" else "Force HTTPS Connection Upgrade",
                            checked = httpsUpgrade,
                            onCheckedChange = { httpsUpgrade = it }
                        )
                        SettingSwitchRow(
                            title = if (isPersian) "پاکسازی حافظه پنهان (Cache) هنگام بستن" else "Clear Cache Automatically on Exit",
                            checked = clearCacheOnExit,
                            onCheckedChange = { clearCacheOnExit = it }
                        )
                        SettingSwitchRow(
                            title = if (isPersian) "پاکسازی کوکی‌ها هنگام بستن" else "Clear Cookies Automatically on Exit",
                            checked = clearCookiesOnExit,
                            onCheckedChange = { clearCookiesOnExit = it }
                        )
                        SettingSwitchRow(
                            title = if (isPersian) "پاکسازی LocalStorage هنگام بستن" else "Clear DOM Storage on Exit",
                            checked = clearStorageOnExit,
                            onCheckedChange = { clearStorageOnExit = it }
                        )
                        SettingSwitchRow(
                            title = if (isPersian) "باز کردن لینک‌های خارج از دامنه در مرورگر اصلی" else "Open External Domain Links in System Browser",
                            checked = openExternalInBrowser,
                            onCheckedChange = { openExternalInBrowser = it }
                        )
                        SettingSwitchRow(
                            title = if (isPersian) "قفل بیومتریک / پین هنگام باز کردن این لایت‌اپ" else "Require Biometric / PIN Authentication to Open",
                            checked = biometricLock,
                            onCheckedChange = { biometricLock = it }
                        )

                        Text(
                            text = if (isPersian) "سیاست پنجره‌های پاپ‌آپ:" else "Pop-up Handling Policy:",
                            style = MaterialTheme.typography.labelLarge
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PopupPolicy.entries.forEach { policy ->
                                FilterChip(
                                    selected = popupPolicy == policy,
                                    onClick = { popupPolicy = policy },
                                    label = { Text(if (isPersian) policy.labelFa else policy.labelEn) }
                                )
                            }
                        }

                        PermissionSelectorRow(
                            label = if (isPersian) "مجوز مکان‌یابی (Geolocation):" else "Geolocation Permission:",
                            current = allowGeolocation,
                            isPersian = isPersian,
                            onSelect = { allowGeolocation = it }
                        )
                        PermissionSelectorRow(
                            label = if (isPersian) "مجوز دوربین (Camera):" else "Camera Permission:",
                            current = allowCamera,
                            isPersian = isPersian,
                            onSelect = { allowCamera = it }
                        )
                        PermissionSelectorRow(
                            label = if (isPersian) "مجوز میکروفون (Microphone):" else "Microphone Permission:",
                            current = allowMicrophone,
                            isPersian = isPersian,
                            onSelect = { allowMicrophone = it }
                        )
                        PermissionSelectorRow(
                            label = if (isPersian) "اعلان‌های جاوااسکریپت وب (W3C Notification API):" else "Web Notification API Bridge:",
                            current = allowWebNotifications,
                            isPersian = isPersian,
                            onSelect = { allowWebNotifications = it }
                        )
                    }

                    else -> EditorSectionCard(title = stringResource(R.string.section_notifications)) {
                        Text(
                            text = stringResource(R.string.webview_limitation_notifications),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = feedMonitorUrl,
                            onValueChange = { feedMonitorUrl = it },
                            label = { Text(if (isPersian) "آدرس خوراک خبری RSS / Atom جهت پایش پس‌زمینه" else "RSS / Atom Feed URL for Background Alerts") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = if (isPersian) "فاصله زمانی بررسی خوراک:" else "Background Feed Poll Interval:",
                            style = MaterialTheme.typography.labelLarge
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0 to "Off", 30 to "30m", 60 to "1h", 180 to "3h", 360 to "6h").forEach { (mins, label) ->
                                FilterChip(
                                    selected = feedIntervalMinutes == mins,
                                    onClick = { feedIntervalMinutes = mins },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PermissionSelectorRow(
    label: String,
    current: PermissionSetting,
    isPersian: Boolean,
    onSelect: (PermissionSetting) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PermissionSetting.entries.forEach { perm ->
                FilterChip(
                    selected = current == perm,
                    onClick = { onSelect(perm) },
                    label = { Text(if (isPersian) perm.labelFa else perm.labelEn) }
                )
            }
        }
    }
}
