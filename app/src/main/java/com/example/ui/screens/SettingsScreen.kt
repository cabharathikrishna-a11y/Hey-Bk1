package com.example.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.HeyBkSettings
import com.example.domain.model.MainSection
import com.example.ui.theme.LocalHeyBkPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: HeyBkSettings,
    onToggleDarkTheme: (Boolean) -> Unit,
    onUpdateReminder: (enabled: Boolean, hour: Int, minute: Int) -> Unit,
    onUpdateDisplayName: (String) -> Unit,
    onExportBackup: (Uri) -> Unit,
    onImportBackup: (Uri) -> Unit,
    onNavigateSection: (MainSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalHeyBkPalette.current
    var showTimePicker by remember { mutableStateOf(false) }
    var nameDraft by remember(settings.displayName) { mutableStateOf(settings.displayName) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        onUpdateReminder(granted, settings.reminderHour, settings.reminderMinute)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> uri?.let(onExportBackup) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onImportBackup) }

    val formattedTime = remember(settings.reminderHour, settings.reminderMinute) {
        val h12 = if (settings.reminderHour % 12 == 0) 12 else settings.reminderHour % 12
        val amPm = if (settings.reminderHour < 12) "AM" else "PM"
        String.format(Locale.getDefault(), "%d:%02d %s", h12, settings.reminderMinute, amPm)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.masterPaneBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings & Backup", style = MaterialTheme.typography.headlineLarge, color = palette.textPrimary)

        // Daily Reminder Card (Default 6:00 PM)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(palette.cardSurface)
                .border(1.dp, palette.hairline, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.NotificationsActive, null, tint = palette.accentAmber)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Daily Reminder", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                        Text("Remind me to write every day", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    }
                }
                Switch(
                    checked = settings.reminderEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onUpdateReminder(enabled, settings.reminderHour, settings.reminderMinute)
                        }
                    },
                    modifier = Modifier.testTag("settings_reminder_switch")
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.elevatedTile)
                    .clickable { showTimePicker = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("settings_reminder_time"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, null, tint = palette.accentBlue)
                    Spacer(Modifier.width(10.dp))
                    Text("Reminder Time", style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary)
                }
                Text(formattedTime, style = MaterialTheme.typography.titleSmall, color = palette.accentBlue)
            }
        }

        // Export & Import Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(palette.cardSurface)
                .border(1.dp, palette.hairline, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Export & Import Data", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
            Text(
                text = "Save a complete portable ZIP backup of your books, entries, version history, tags, people, and attached media—or restore from a backup file on another device.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                        exportLauncher.launch("HeyBk_Backup_$stamp.zip")
                    },
                    modifier = Modifier.weight(1f).testTag("settings_export_backup")
                ) {
                    Icon(Icons.Outlined.Backup, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Export ZIP")
                }
                OutlinedButton(
                    onClick = {
                        importLauncher.launch(arrayOf("application/zip", "application/json", "*/*"))
                    },
                    modifier = Modifier.weight(1f).testTag("settings_import_backup")
                ) {
                    Icon(Icons.Outlined.FileDownload, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Import")
                }
            }
        }

        // Personalization & Theme
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(palette.cardSurface)
                .border(1.dp, palette.hairline, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Appearance & Profile", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.DarkMode, null, tint = palette.textSecondary)
                    Spacer(Modifier.width(10.dp))
                    Text("Dark Slate Theme", style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary)
                }
                Switch(
                    checked = settings.darkTheme,
                    onCheckedChange = onToggleDarkTheme,
                    modifier = Modifier.testTag("settings_dark_theme_switch")
                )
            }
            HorizontalDivider(color = palette.hairline)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    label = { Text("Today greeting name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { onUpdateDisplayName(nameDraft) }) {
                    Text("Save")
                }
            }
        }

        // Quick Workspace Links
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(palette.cardSurface)
                .border(1.dp, palette.hairline, RoundedCornerShape(16.dp))
                .padding(8.dp)
        ) {
            SettingsNavRow("People & Contacts", Icons.Outlined.PeopleOutline) {
                onNavigateSection(MainSection.PEOPLE)
            }
            SettingsNavRow("Tags Directory", Icons.Outlined.Label) {
                onNavigateSection(MainSection.TAGS)
            }
            SettingsNavRow("Trash", Icons.Outlined.DeleteOutline) {
                onNavigateSection(MainSection.TRASH)
            }
        }
    }

    if (showTimePicker) {
        val tpState = rememberTimePickerState(
            initialHour = settings.reminderHour,
            initialMinute = settings.reminderMinute
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Daily Reminder Time") },
            text = { TimePicker(state = tpState) },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateReminder(true, tpState.hour, tpState.minute)
                    showTimePicker = false
                }) { Text("Set Time") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsNavRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val palette = LocalHeyBkPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = palette.textSecondary)
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, color = palette.textPrimary)
    }
}
