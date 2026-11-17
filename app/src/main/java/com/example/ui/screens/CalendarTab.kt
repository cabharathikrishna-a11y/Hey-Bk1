package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.JournalEntryDetail
import com.example.ui.state.dayKeyOf
import com.example.ui.theme.LocalHeyBkPalette
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class MonthSpec(
    val year: Int,
    val monthZeroIndexed: Int // 0..11
)

/**
 * Matches Screenshot 3:
 * Vertically scrollable monthly calendar across years.
 * - Photo background on dates that have a photo
 * - Amber outline on Today
 * - Subtle dot / slate tint on dates with text-only entries
 */
@Composable
fun CalendarTab(
    entries: List<JournalEntryDetail>,
    onDayClicked: (dayKey: Long, dayMillis: Long, matchingEntries: List<JournalEntryDetail>) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalHeyBkPalette.current
    val nowCal = remember { Calendar.getInstance() }
    val currentYear = nowCal.get(Calendar.YEAR)
    val currentMonth = nowCal.get(Calendar.MONTH)
    val todayKey = remember { dayKeyOf(System.currentTimeMillis()) }

    val earliestEntryYear = remember(entries) {
        entries.minOfOrNull {
            Calendar.getInstance().apply { timeInMillis = it.entryTimestamp }.get(Calendar.YEAR)
        } ?: currentYear
    }
    var startYear by remember(earliestEntryYear) {
        mutableIntStateOf(minOf(earliestEntryYear, currentYear - 1))
    }
    val endYear = currentYear + 1

    val months = remember(startYear, endYear) {
        buildList {
            for (y in startYear..endYear) {
                for (m in 0..11) {
                    add(MonthSpec(y, m))
                }
            }
        }
    }

    val currentMonthIndex = remember(months, currentYear, currentMonth) {
        months.indexOfFirst { it.year == currentYear && it.monthZeroIndexed == currentMonth }
            .coerceAtLeast(0)
    }
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        listState.scrollToItem(currentMonthIndex)
    }

    val entriesByDay = remember(entries) {
        entries.groupBy { dayKeyOf(it.entryTimestamp) }
    }

    val monthTitleFmt = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(key = "load_earlier_years") {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                OutlinedButton(
                    onClick = { startYear -= 2 },
                    modifier = Modifier.testTag("calendar_load_earlier")
                ) {
                    Text("Load ${startYear - 2}–${startYear - 1}")
                }
            }
        }

        items(months, key = { "${it.year}_${it.monthZeroIndexed}" }) { spec ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, spec.year)
                set(Calendar.MONTH, spec.monthZeroIndexed)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            // Calendar.SUNDAY = 1 ... SATURDAY = 7 -> Monday-first offset 0..6
            val firstDayOffset = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
            val monthLabel = monthTitleFmt.format(Date(cal.timeInMillis))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.headlineSmall,
                    color = palette.textPrimary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                )

                val totalCells = firstDayOffset + daysInMonth
                val rows = (totalCells + 6) / 7

                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (r in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            for (c in 0 until 7) {
                                val cellIndex = r * 7 + c
                                val dayNum = cellIndex - firstDayOffset + 1
                                if (dayNum in 1..daysInMonth) {
                                    val cellCal = (cal.clone() as Calendar).apply {
                                        set(Calendar.DAY_OF_MONTH, dayNum)
                                    }
                                    val dayKey = (spec.year * 10000L) +
                                        (spec.monthZeroIndexed * 100L) +
                                        dayNum
                                    val dayEntries = entriesByDay[dayKey].orEmpty()
                                    val dayPhoto = dayEntries.firstNotNullOfOrNull { it.firstPhoto }
                                    val isToday = dayKey == todayKey

                                    CalendarDayCell(
                                        dayNumber = dayNum,
                                        photoUri = dayPhoto?.localUri,
                                        hasEntries = dayEntries.isNotEmpty(),
                                        isToday = isToday,
                                        onClick = {
                                            onDayClicked(dayKey, cellCal.timeInMillis, dayEntries)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
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
private fun CalendarDayCell(
    dayNumber: Int,
    photoUri: String?,
    hasEntries: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalHeyBkPalette.current
    val borderColor = when {
        isToday -> palette.accentAmber
        hasEntries && photoUri == null -> palette.accentBlue.copy(alpha = 0.45f)
        else -> Color.Transparent
    }
    val cellBg = when {
        hasEntries -> palette.selectedCard
        else -> palette.cardSurface
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(cellBg)
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(2.dp, borderColor, RoundedCornerShape(6.dp))
                } else Modifier
            )
            .clickable(onClick = onClick)
            .testTag("calendar_day_$dayNumber"),
        contentAlignment = Alignment.Center
    ) {
        if (photoUri != null) {
            AsyncImage(
                model = photoUri,
                contentDescription = "Entry photo for day $dayNumber",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.15f),
                                Color.Black.copy(alpha = 0.45f)
                            )
                        )
                    )
            )
        }

        Text(
            text = dayNumber.toString(),
            fontSize = 15.sp,
            fontWeight = if (photoUri != null || isToday) FontWeight.Bold else FontWeight.Medium,
            color = if (photoUri != null) Color.White else palette.textPrimary
        )

        if (hasEntries && photoUri == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(palette.accentBlue)
            )
        }
    }
}
