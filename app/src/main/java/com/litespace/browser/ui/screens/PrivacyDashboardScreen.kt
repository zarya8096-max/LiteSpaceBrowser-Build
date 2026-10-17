package com.litespace.browser.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.litespace.browser.R
import com.litespace.browser.domain.model.ContentFilterRule
import com.litespace.browser.domain.model.GlobalAppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyDashboardScreen(
    settings: GlobalAppSettings,
    filterRules: List<ContentFilterRule>,
    isPersian: Boolean,
    onToggleGlobalAdBlock: (Boolean) -> Unit,
    onToggleGlobalHttpsOnly: (Boolean) -> Unit,
    onToggleGlobalThirdPartyCookies: (Boolean) -> Unit,
    onToggleGlobalClearOnExit: (Boolean) -> Unit,
    onAddFilterRule: (pattern: String, isException: Boolean) -> Unit,
    onToggleRule: (ContentFilterRule) -> Unit,
    onDeleteRule: (Long) -> Unit
) {
    var newRulePattern by remember { mutableStateOf("") }
    var isExceptionRule by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.privacy_shield_title),
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "${settings.totalBlockedRequests}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (isPersian) {
                                    "تعداد کل درخواست‌های تبلیغاتی و ردیاب مسدودشده (+۹۰ قانون پیش‌فرض + قوانین سفارشی)"
                                } else {
                                    "Total Ad & Tracker Requests Blocked (90+ built-in EasyList domains + custom rules)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isPersian) "سیاست‌های سراسری حریم خصوصی" else "Global Privacy Enforcement",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        SettingSwitchRow(
                            title = if (isPersian) "مسدودکننده سراسری تبلیغات و ردیاب‌ها" else "Global Ad & Tracker Blocker",
                            checked = settings.globalAdBlockEnabled,
                            onCheckedChange = onToggleGlobalAdBlock
                        )
                        SettingSwitchRow(
                            title = if (isPersian) "ارتقای اجباری تمام ارتباطات به HTTPS" else "Global HTTPS-Only Upgrade",
                            checked = settings.globalHttpsOnly,
                            onCheckedChange = onToggleGlobalHttpsOnly
                        )
                        SettingSwitchRow(
                            title = if (isPersian) "مسدودسازی سراسری کوکی‌های شخص ثالث" else "Block Third-Party Cookies Globally",
                            checked = settings.globalBlockThirdPartyCookies,
                            onCheckedChange = onToggleGlobalThirdPartyCookies
                        )
                        SettingSwitchRow(
                            title = if (isPersian) "پاکسازی خودکار داده‌های مرور هنگام خروج" else "Clear Browsing Data on Exit by Default",
                            checked = settings.globalClearOnExit,
                            onCheckedChange = onToggleGlobalClearOnExit
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.webview_limitations_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = stringResource(R.string.webview_limitation_isolation),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.webview_limitation_notifications),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isPersian) "افزودن قانون مسدودسازی یا استثنا" else "Add Custom Content Filter Rule",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = newRulePattern,
                            onValueChange = { newRulePattern = it },
                            label = { Text("Domain or URL pattern (e.g. tracker.example.com)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = !isExceptionRule,
                                    onClick = { isExceptionRule = false },
                                    label = { Text(if (isPersian) "مسدودسازی (Block)" else "Block Rule") }
                                )
                                FilterChip(
                                    selected = isExceptionRule,
                                    onClick = { isExceptionRule = true },
                                    label = { Text(if (isPersian) "استثنا (Allow @@)" else "Allow Exception (@@)") }
                                )
                            }
                            Button(
                                onClick = {
                                    if (newRulePattern.isNotBlank()) {
                                        onAddFilterRule(newRulePattern, isExceptionRule)
                                        newRulePattern = ""
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(if (isPersian) "افزودن" else "Add")
                            }
                        }
                    }
                }
            }

            items(filterRules, key = { it.id }) { rule ->
                Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = (if (rule.isException) "@@||" else "||") + rule.pattern + "^",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isPersian) "تعداد برخورد: ${rule.hitCount}" else "Matched hits: ${rule.hitCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = rule.isEnabled,
                            onCheckedChange = { onToggleRule(rule) }
                        )
                        IconButton(onClick = { onDeleteRule(rule.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete rule")
                        }
                    }
                }
            }
        }
    }
}
