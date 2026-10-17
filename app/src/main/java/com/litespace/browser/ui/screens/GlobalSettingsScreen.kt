package com.litespace.browser.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.litespace.browser.R
import com.litespace.browser.domain.model.AppThemeMode
import com.litespace.browser.domain.model.GlobalAppSettings

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GlobalSettingsScreen(
    settings: GlobalAppSettings,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
    onSelectLanguage: (String) -> Unit,
    onToggleBiometricGlobal: (Boolean) -> Unit,
    onUpdatePinCode: (String) -> Unit
) {
    val isPersian = settings.languageCode.startsWith("fa")
    var pinInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_theme),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { onSelectThemeMode(mode) },
                                label = { Text(if (isPersian) mode.labelFa else mode.labelEn) }
                            )
                        }
                    }
                    SettingSwitchRow(
                        title = stringResource(R.string.settings_dynamic_color),
                        checked = settings.dynamicColorEnabled,
                        onCheckedChange = onToggleDynamicColor
                    )
                }
            }

            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterChip(
                            selected = settings.languageCode == "en",
                            onClick = { onSelectLanguage("en") },
                            label = { Text(stringResource(R.string.settings_language_en)) }
                        )
                        FilterChip(
                            selected = settings.languageCode == "fa",
                            onClick = { onSelectLanguage("fa") },
                            label = { Text(stringResource(R.string.settings_language_fa)) }
                        )
                    }
                }
            }

            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isPersian) "قفل امنیتی بیومتریک و رمز پین (SHA-256)" else "Biometric & PIN Protection (SHA-256)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    SettingSwitchRow(
                        title = if (isPersian) "درخواست احراز هویت هنگام ورود به برنامه اصلی" else "Require Biometric / PIN on App Launch",
                        checked = settings.biometricGlobalEnabled,
                        onCheckedChange = onToggleBiometricGlobal
                    )
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it.filter { ch -> ch.isDigit() }.take(8) },
                        label = {
                            Text(
                                if (settings.pinCodeSha256.isNotBlank()) {
                                    if (isPersian) "تغییر رمز پین (۴ تا ۸ رقم)" else "Update Security PIN (4–8 digits, currently active)"
                                } else {
                                    if (isPersian) "تعیین رمز پین جدید (۴ تا ۸ رقم)" else "Set New Security PIN (4–8 digits)"
                                }
                            )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                if (pinInput.length >= 4) {
                                    onUpdatePinCode(pinInput)
                                    pinInput = ""
                                }
                            }
                        ) {
                            Text(if (isPersian) "ذخیره پین" else "Save PIN")
                        }
                        if (settings.pinCodeSha256.isNotBlank()) {
                            OutlinedButton(onClick = { onUpdatePinCode("") }) {
                                Text(if (isPersian) "حذف پین" else "Remove PIN")
                            }
                        }
                    }
                }
            }
        }
    }
}
