package com.litespace.browser.ui.theme

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.litespace.browser.core.shortcut.ShortcutPinManager
import com.litespace.browser.domain.model.AppThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B57D0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E3FD),
    onPrimaryContainer = Color(0xFF041E49),
    secondary = Color(0xFF00796B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2DFDB),
    onSecondaryContainer = Color(0xFF00201C),
    tertiary = Color(0xFF7B1FA2),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3E5F5),
    onTertiaryContainer = Color(0xFF310047),
    background = Color(0xFFF4F7FC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE6ECF5),
    onSurfaceVariant = Color(0xFF475569)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF002E6B),
    primaryContainer = Color(0xFF0F3D82),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFF4DB6AC),
    onSecondary = Color(0xFF003730),
    secondaryContainer = Color(0xFF005047),
    onSecondaryContainer = Color(0xFFB2DFDB),
    tertiary = Color(0xFFCE93D8),
    onTertiary = Color(0xFF490066),
    tertiaryContainer = Color(0xFF63197E),
    onTertiaryContainer = Color(0xFFF3E5F5),
    background = Color(0xFF090D16),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8)
)

private val AmoledColors = DarkColors.copy(
    background = Color(0xFF000000),
    surface = Color(0xFF070A10),
    surfaceVariant = Color(0xFF121824)
)

private val LiteSpaceTypography = Typography(
    headlineMedium = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

private val SquircleShape = RoundedCornerShape(percent = 28)
private val MicroChipShape = RoundedCornerShape(8.dp)
private val TileShape = RoundedCornerShape(16.dp)

fun parseHexColorSafely(hex: String, fallback: Color = Color(0xFF0B57D0)): Color {
    if (hex.isBlank()) return fallback
    return runCatching {
        Color(android.graphics.Color.parseColor(hex.trim()))
    }.getOrDefault(fallback)
}

/**
 * Crucial Crash-Prevention Wrapper:
 * Wrapping `base` in `ContextWrapper(base)` preserves the `ComponentActivity` /
 * `ActivityResultRegistryOwner` chain and Activity `Window` token so
 * `rememberLauncherForActivityResult`, `ModalBottomSheet`, and `AlertDialog` never crash!
 */
private class ActivityPreservingLocaleContextWrapper(
    base: Context,
    private val localizedResources: Resources
) : ContextWrapper(base) {
    override fun getResources(): Resources = localizedResources
}

fun applyLocaleToContext(base: Context, languageCode: String): Context {
    val targetLang = languageCode.lowercase().trim().ifBlank { "en" }
    val locale = Locale(targetLang)
    Locale.setDefault(locale)
    val config = Configuration(base.resources.configuration)
    config.setLocale(locale)
    config.setLayoutDirection(locale)
    val configContext = base.createConfigurationContext(config)
    return ActivityPreservingLocaleContextWrapper(base, configContext.resources)
}

@Composable
fun LiteSpaceTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    languageCode: String = "en",
    customSeedHex: String? = null,
    content: @Composable () -> Unit
) {
    val baseContext = LocalContext.current
    val localizedContext = remember(baseContext, languageCode) {
        applyLocaleToContext(baseContext, languageCode)
    }

    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK, AppThemeMode.AMOLED -> true
    }

    val baseScheme: ColorScheme = remember(themeMode, dynamicColor, useDark, baseContext) {
        when {
            themeMode == AppThemeMode.AMOLED -> AmoledColors
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (useDark) dynamicDarkColorScheme(baseContext) else dynamicLightColorScheme(baseContext)
            }
            useDark -> DarkColors
            else -> LightColors
        }
    }

    val finalScheme = remember(baseScheme, customSeedHex, useDark) {
        if (!customSeedHex.isNullOrBlank()) {
            val accent = parseHexColorSafely(customSeedHex, baseScheme.primary)
            baseScheme.copy(
                primary = accent,
                primaryContainer = accent.copy(alpha = if (useDark) 0.28f else 0.16f)
            )
        } else {
            baseScheme
        }
    }

    val layoutDirection = if (languageCode.lowercase().startsWith("fa") ||
        languageCode.lowercase().startsWith("ar")
    ) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalLayoutDirection provides layoutDirection
    ) {
        MaterialTheme(
            colorScheme = finalScheme,
            typography = LiteSpaceTypography,
            content = content
        )
    }
}

/**
 * 120fps Optimized Squircle Icon:
 * - Memoizes color parsing & gradient brush per `colorHex`
 * - Decodes `customIconUri` asynchronously on `Dispatchers.IO` via `produceState`
 *   so scrolling and typing never block the Main UI thread!
 */
@Composable
fun LiteAppSquircleIcon(
    iconText: String,
    colorHex: String,
    customIconUri: String? = null,
    size: Dp = 50.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val gradientBrush = remember(colorHex) {
        val baseColor = parseHexColorSafely(colorHex)
        Brush.linearGradient(
            colors = listOf(
                baseColor.copy(
                    red = (baseColor.red * 1.18f).coerceIn(0f, 1f),
                    green = (baseColor.green * 1.18f).coerceIn(0f, 1f),
                    blue = (baseColor.blue * 1.18f).coerceIn(0f, 1f)
                ),
                baseColor,
                baseColor.copy(
                    red = (baseColor.red * 0.72f).coerceIn(0f, 1f),
                    green = (baseColor.green * 0.72f).coerceIn(0f, 1f),
                    blue = (baseColor.blue * 0.72f).coerceIn(0f, 1f)
                )
            ),
            start = Offset.Zero,
            end = Offset(120f, 120f)
        )
    }

    val customBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = customIconUri) {
        value = if (!customIconUri.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                runCatching {
                    ShortcutPinManager.renderLiteAppIconBitmap(
                        context = context.applicationContext,
                        iconText = iconText,
                        colorHex = colorHex,
                        customIconUri = customIconUri,
                        sizePx = 144
                    ).asImageBitmap()
                }.getOrNull()
            }
        } else {
            null
        }
    }

    val bmp = customBitmap
    if (bmp != null) {
        Image(
            bitmap = bmp,
            contentDescription = iconText,
            modifier = modifier
                .size(size)
                .shadow(4.dp, SquircleShape)
                .clip(SquircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.25f), SquircleShape)
        )
    } else {
        val displayLabel = remember(iconText) {
            iconText.trim().take(2).uppercase().ifBlank { "LA" }
        }
        Box(
            modifier = modifier
                .size(size)
                .shadow(4.dp, SquircleShape)
                .clip(SquircleShape)
                .background(gradientBrush)
                .border(1.dp, Color.White.copy(alpha = 0.28f), SquircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayLabel,
                color = Color.White,
                style = if (size >= 56.dp) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.titleMedium
                },
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun FeatureMicroChip(
    label: String,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Box(
        modifier = Modifier
            .clip(MicroChipShape)
            .background(containerColor)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
fun QuickControlGridTile(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    isActive: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }
    val fg = if (isActive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        color = bg,
        contentColor = fg,
        shape = TileShape,
        modifier = modifier
            .clip(TileShape)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isActive) MaterialTheme.colorScheme.onPrimary else fg,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = fg.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun LiteSpacePullToRefreshBox(
    enabled: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var pullOffsetPx by remember { mutableFloatStateOf(0f) }
    val triggerThresholdPx = 190f

    val nestedScrollConnection = remember(enabled) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!enabled) return Offset.Zero
                if (pullOffsetPx > 0f && available.y < 0f) {
                    val consumed = maxOf(available.y, -pullOffsetPx)
                    pullOffsetPx = (pullOffsetPx + consumed).coerceAtLeast(0f)
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!enabled) return Offset.Zero
                if (source == NestedScrollSource.Drag && available.y > 0f && consumed.y == 0f) {
                    pullOffsetPx = (pullOffsetPx + available.y * 0.45f).coerceAtMost(280f)
                    return Offset(0f, available.y * 0.45f)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                if (enabled && pullOffsetPx >= triggerThresholdPx) {
                    onRefresh()
                }
                pullOffsetPx = 0f
                return androidx.compose.ui.unit.Velocity.Zero
            }
        }
    }

    val animatedOffset by animateFloatAsState(
        targetValue = if (isRefreshing) 110f else pullOffsetPx,
        animationSpec = spring(),
        label = "pull_to_refresh_offset"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        content()

        if (animatedOffset > 12f || isRefreshing) {
            Surface(
                shape = CircleShape,
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, (animatedOffset * 0.55f).roundToInt() - 24) }
                    .size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
