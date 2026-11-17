package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.JournalEntryDetail
import com.example.domain.model.MediaType
import com.example.ui.state.dayKeyOf
import com.example.ui.theme.LocalHeyBkPalette
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Matches Screenshot 4 ("Today" dashboard):
 * - Today header + date + calendar jump + < > day stepper
 * - "X Entry" and "Y On This Day" quick cards
 * - Daily Chat / Prompt card with sparkle icon & action bar
 * - Moments section (Photos, People Tagged)
 * - + New entry FAB
 */
@Composable
fun TodayScreen(
    selectedMillis: Long,
    displayName: String,
    allEntries: List<JournalEntryDetail>,
    dailyPrompt: String,
    onStepDate: (Int) -> Unit,
    onResetToToday: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onStartNewEntryForDate: (Long, String?) -> Unit,
    onOpenSettings: () -> Unit,
    onShowAiInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalHeyBkPalette.current
    val dateFmt = SimpleDateFormat("EEE, dd MMM, yyyy", Locale.getDefault())
    val selectedDayKey = dayKeyOf(selectedMillis)

    val cal = Calendar.getInstance().apply { timeInMillis = selectedMillis }
    val selYear = cal.get(Calendar.YEAR)
    val selMonth = cal.get(Calendar.MONTH)
    val selDay = cal.get(Calendar.DAY_OF_MONTH)

    val todayEntries = allEntries.filter { dayKeyOf(it.entryTimestamp) == selectedDayKey }
    val onThisDayEntries = allEntries.filter { entry ->
        val ec = Calendar.getInstance().apply { timeInMillis = entry.entryTimestamp }
        ec.get(Calendar.MONTH) == selMonth &&
            ec.get(Calendar.DAY_OF_MONTH) == selDay &&
            ec.get(Calendar.YEAR) < selYear
    }
    val photosTodayCount = todayEntries.sumOf { e -> e.media.count { it.type == MediaType.PHOTO } }
    val peopleTodayCount = todayEntries.flatMap { it.people }.distinctBy { it.id }.size

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.masterPaneBackground)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.headlineLarge,
                        color = palette.textPrimary
                    )
                    Text(
                        text = dateFmt.format(Date(selectedMillis)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onResetToToday,
                        modifier = Modifier.testTag("today_jump_current")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = "Jump to today",
                            tint = palette.textPrimary
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(palette.cardSurface)
                                .clickable { onStepDate(-1) }
                                .testTag("today_prev_day"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = "Previous day",
                                tint = palette.textPrimary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(palette.cardSurface)
                                .clickable { onStepDate(1) }
                                .testTag("today_next_day"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "Next day",
                                tint = palette.textPrimary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TodaySummaryPill(
                    icon = Icons.Outlined.EditNote,
                    text = "${todayEntries.size} ${if (todayEntries.size == 1) "Entry" else "Entries"}",
                    onClick = {
                        val first = todayEntries.firstOrNull()
                        if (first != null) onOpenEntry(first.id) else onStartNewEntryForDate(selectedMillis, null)
                    },
                    modifier = Modifier.weight(1f)
                )
                TodaySummaryPill(
                    icon = Icons.Outlined.History,
                    text = "${onThisDayEntries.size} On This Day",
                    onClick = {
                        onThisDayEntries.firstOrNull()?.let { onOpenEntry(it.id) }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(22.dp))

            Text(
                text = "Daily Prompt & Chat",
                style = MaterialTheme.typography.titleLarge,
                color = palette.textPrimary
            )
            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.cardSurface)
                    .border(1.dp, palette.hairline, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = palette.accentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Hey $displayName — $dailyPrompt",
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textPrimary
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.selectedCard)
                        .clickable { onStartNewEntryForDate(selectedMillis, dailyPrompt) }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("today_write_prompt_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            tint = palette.accentBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Write About Today",
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.textPrimary
                        )
                    }
                    Text(
                        text = "AI Soon",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.accentBlue,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onShowAiInfo)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Moments",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Reminder & journal settings",
                        tint = palette.textSecondary
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            MomentsCountRow(
                icon = Icons.Outlined.PhotoLibrary,
                label = "Photos",
                count = photosTodayCount
            )
            Spacer(Modifier.height(8.dp))
            MomentsCountRow(
                icon = Icons.Outlined.PeopleOutline,
                label = "People Tagged",
                count = peopleTodayCount
            )

            if (onThisDayEntries.isNotEmpty()) {
                Spacer(Modifier.height(22.dp))
                Text(
                    text = "On This Day in Previous Years",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.accentAmber
                )
                Spacer(Modifier.height(8.dp))
                onThisDayEntries.forEach { past ->
                    val yearFmt = SimpleDateFormat("yyyy", Locale.getDefault())
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.cardSurface)
                            .border(1.dp, palette.hairline, RoundedCornerShape(14.dp))
                            .clickable { onOpenEntry(past.id) }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${yearFmt.format(Date(past.entryTimestamp))} • ${past.bookTitle}",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.accentAmber
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = past.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.textPrimary
                            )
                            if (past.plainTextCache.isNotBlank()) {
                                Text(
                                    text = past.plainTextCache.take(90),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }

            if (todayEntries.isNotEmpty()) {
                Spacer(Modifier.height(22.dp))
                Text(
                    text = "Entries on This Date",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary
                )
                Spacer(Modifier.height(8.dp))
                todayEntries.forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.cardSurface)
                            .clickable { onOpenEntry(entry.id) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.textPrimary
                            )
                            if (entry.plainTextCache.isNotBlank()) {
                                Text(
                                    text = entry.plainTextCache.take(100),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(88.dp))
        }

        ExtendedFloatingActionButton(
            onClick = { onStartNewEntryForDate(selectedMillis, null) },
            containerColor = palette.fabContainer,
            contentColor = palette.fabContent,
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("New entry") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .navigationBarsPadding()
                .testTag("today_new_entry_fab")
        )
    }
}

@Composable
private fun TodaySummaryPill(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalHeyBkPalette.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(palette.cardSurface)
            .border(1.dp, palette.hairline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = palette.textPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MomentsCountRow(
    icon: ImageVector,
    label: String,
    count: Int
) {
    val palette = LocalHeyBkPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.cardSurface)
            .border(1.dp, palette.hairline, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, color = palette.textPrimary)
        }
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyLarge,
            color = palette.textSecondary
        )
    }
}
