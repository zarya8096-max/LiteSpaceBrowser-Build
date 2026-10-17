package com.litespace.browser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.litespace.browser.R
import com.litespace.browser.domain.model.CatalogTemplate
import com.litespace.browser.domain.model.CatalogTemplates
import com.litespace.browser.domain.model.LiteAppCategory
import com.litespace.browser.domain.model.LiteAppConfig
import com.litespace.browser.ui.theme.FeatureMicroChip
import com.litespace.browser.ui.theme.LiteAppSquircleIcon
import com.litespace.browser.ui.theme.parseHexColorSafely

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    liteApps: List<LiteAppConfig>,
    searchQuery: String,
    selectedCategory: LiteAppCategory?,
    languageCode: String,
    totalBlockedCount: Long,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (LiteAppCategory?) -> Unit,
    onToggleLanguage: () -> Unit,
    onCreateNewApp: (initialUrl: String) -> Unit,
    onEditApp: (LiteAppConfig) -> Unit,
    onLaunchApp: (LiteAppConfig, Boolean) -> Unit,
    onQuickIncognitoUrl: (String) -> Unit,
    onPinShortcut: (LiteAppConfig) -> Unit,
    onToggleFavorite: (LiteAppConfig) -> Unit,
    onDeleteApp: (LiteAppConfig) -> Unit,
    onAddTemplate: (CatalogTemplate) -> Unit
) {
    val isPersian = languageCode.startsWith("fa")
    var compactListView by remember { mutableStateOf(false) }
    val distinctProfiles = remember(liteApps) {
        liteApps.map { it.profileId }.distinct().size.coerceAtLeast(1)
    }
    val favoriteApps = remember(liteApps) {
        liteApps.filter { it.isFavorite }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF0B57D0), Color(0xFF00897B))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("LS", color = Color.White, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = stringResource(R.string.app_tagline),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { compactListView = !compactListView }) {
                        Icon(
                            imageVector = if (compactListView) Icons.Default.GridView else Icons.Default.ViewAgenda,
                            contentDescription = "Toggle View Mode"
                        )
                    }
                    AssistChip(
                        onClick = onToggleLanguage,
                        label = {
                            Text(
                                if (isPersian) "EN" else "فارسی",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Language,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onCreateNewApp("") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.create_lite_app), fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = if (compactListView) GridCells.Fixed(1) else GridCells.Adaptive(minSize = 162.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 6.dp,
                bottom = innerPadding.calculateBottomPadding() + 92.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                HeroCommandBanner(
                    isPersian = isPersian,
                    totalAppsCount = liteApps.size,
                    totalBlockedCount = totalBlockedCount,
                    isolatedProfilesCount = distinctProfiles,
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    onQuickIncognitoUrl = onQuickIncognitoUrl,
                    onCreateNewApp = onCreateNewApp
                )
            }

            if (favoriteApps.isNotEmpty() && searchQuery.isBlank() && selectedCategory == null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        Text(
                            text = if (isPersian) "★ لایت‌اپ‌های برگزیده" else "★ Pinned Favorites",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(favoriteApps, key = { "fav_${it.id}" }) { favApp ->
                                Surface(
                                    onClick = { onLaunchApp(favApp, false) },
                                    shape = RoundedCornerShape(16.dp),
                                    tonalElevation = 3.dp,
                                    modifier = Modifier.width(135.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        LiteAppSquircleIcon(
                                            iconText = favApp.iconText,
                                            colorHex = favApp.iconColorHex,
                                            customIconUri = favApp.customIconUri,
                                            size = 36.dp
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = favApp.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = favApp.profileId,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { onSelectCategory(null) },
                            label = {
                                Text(
                                    stringResource(R.string.category_all),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        )
                    }
                    items(LiteAppCategory.entries) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = {
                                onSelectCategory(if (selectedCategory == category) null else category)
                            },
                            label = {
                                Text(if (isPersian) category.labelFa else category.labelEn)
                            }
                        )
                    }
                }
            }

            if (liteApps.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.empty_dashboard_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.empty_dashboard_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(liteApps, key = { it.id }) { app ->
                    LiteAppCardItem(
                        app = app,
                        isPersian = isPersian,
                        isCompactRow = compactListView,
                        onOpen = { onLaunchApp(app, false) },
                        onOpenIncognito = { onLaunchApp(app, true) },
                        onEdit = { onEditApp(app) },
                        onPinShortcut = { onPinShortcut(app) },
                        onToggleFavorite = { onToggleFavorite(app) },
                        onDelete = { onDeleteApp(app) }
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Text(
                        text = if (isPersian) "الگوهای آماده لایت‌اپ (نصب با یک لمس)" else "Curated Lite App Templates (1-Tap Add)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(CatalogTemplates.builtInTemplates) { tpl ->
                            val tplColor = parseHexColorSafely(tpl.iconColorHex)
                            ElevatedCard(
                                modifier = Modifier
                                    .width(245.dp)
                                    .clickable { onAddTemplate(tpl) },
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(tplColor, tplColor.copy(alpha = 0.72f))
                                                )
                                            )
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            LiteAppSquircleIcon(
                                                iconText = tpl.iconText,
                                                colorHex = tpl.iconColorHex,
                                                size = 36.dp
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = tpl.name,
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = if (isPersian) tpl.category.labelFa else tpl.category.labelEn,
                                                    color = Color.White.copy(alpha = 0.85f),
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = null,
                                                tint = Color.White
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (isPersian) tpl.descriptionFa else tpl.descriptionEn,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCommandBanner(
    isPersian: Boolean,
    totalAppsCount: Int,
    totalBlockedCount: Long,
    isolatedProfilesCount: Int,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onQuickIncognitoUrl: (String) -> Unit,
    onCreateNewApp: (String) -> Unit
) {
    val heroGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0A2E6E),
            Color(0xFF0E4D92),
            Color(0xFF00695C)
        ),
        start = Offset.Zero,
        end = Offset(900f, 500f)
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(heroGradient)
        ) {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 35.dp, y = (-30).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFF69F0AE)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isPersian) "سپر حریم خصوصی و سندباکس فعال است" else "Privacy Shield & Profile Sandbox Active",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isPersian) {
                                    "ایزوله‌سازی کوکی‌ها، مسدودساز تبلیغات و اسکریپت‌های سفارشی"
                                } else {
                                    "Per-App Cookie Vault • EasyList Blocker • Custom UserScripts"
                                },
                                color = Color.White.copy(alpha = 0.78f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeroMetricPill(
                        value = "$totalAppsCount",
                        label = if (isPersian) "لایت‌اپ فعال" else "Lite Apps",
                        modifier = Modifier.weight(1f)
                    )
                    HeroMetricPill(
                        value = "$totalBlockedCount",
                        label = if (isPersian) "ردیاب مسدودشده" else "Ads Blocked",
                        modifier = Modifier.weight(1f)
                    )
                    HeroMetricPill(
                        value = "$isolatedProfilesCount",
                        label = if (isPersian) "پروفایل ایزوله" else "Profiles",
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            stringResource(R.string.search_placeholder),
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.White
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.White.copy(alpha = 0.14f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.10f),
                        focusedBorderColor = Color.White.copy(alpha = 0.55f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                        cursorColor = Color.White
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                if (searchQuery.trim().isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { onQuickIncognitoUrl(searchQuery.trim()) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.quick_incognito),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        OutlinedButton(
                            onClick = { onCreateNewApp(searchQuery.trim()) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.create_lite_app),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroMetricPill(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
            .padding(vertical = 8.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

@Composable
private fun LiteAppCardItem(
    app: LiteAppConfig,
    isPersian: Boolean,
    isCompactRow: Boolean,
    onOpen: () -> Unit,
    onOpenIncognito: () -> Unit,
    onEdit: () -> Unit,
    onPinShortcut: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    LiteAppSquircleIcon(
                        iconText = app.iconText,
                        colorHex = app.iconColorHex,
                        customIconUri = app.customIconUri,
                        size = if (isCompactRow) 44.dp else 50.dp
                    )
                    if (isCompactRow) {
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = app.startUrl.removePrefix("https://").removePrefix("http://"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (app.biometricLockEnabled) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (app.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (app.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.launch_incognito)) },
                                leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onOpenIncognito()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.pin_to_home)) },
                                leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onPinShortcut()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.configure_app)) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete_app)) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            if (!isCompactRow) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.startUrl.removePrefix("https://").removePrefix("http://"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FeatureMicroChip(label = app.profileId)
                if (app.adBlockEnabled) {
                    FeatureMicroChip(
                        label = if (isPersian) "سپر" else "Shield",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                if (app.forceDarkWeb) {
                    FeatureMicroChip(
                        label = if (isPersian) "تیره" else "Dark",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                if (app.customCss.isNotBlank() || app.customJs.isNotBlank()) {
                    FeatureMicroChip(
                        label = "JS/CSS",
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}
