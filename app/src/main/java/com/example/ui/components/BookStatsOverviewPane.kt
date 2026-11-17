package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.BookStats
import com.example.domain.model.FeedTab
import com.example.domain.model.MediaFilter
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalHeyBkPalette
import com.example.ui.theme.parseColorHex

/**
 * Matches Screenshot 2:
 * Centered colored book glyph, Book Title, Year Span, and the 12-tile Stats Grid:
 * Streak | Entries | Days | Media | On This Day
 * This week | Place | Tag | Video | Photo
 * Audio | File
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookStatsOverviewPane(
    stats: BookStats,
    onTileClick: (StatTileAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalHeyBkPalette.current
    val bookColor = parseColorHex(stats.bookColorHex, palette.accentBlue)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.detailPaneBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 74.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bookColor)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stats.bookTitle,
                style = MaterialTheme.typography.displaySmall,
                color = palette.textPrimary,
                fontWeight = FontWeight.Normal
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stats.yearSpanLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary
            )

            Spacer(Modifier.height(28.dp))

            val tiles = listOf(
                Triple("Streak", stats.streak, StatTileAction.NONE),
                Triple("Entries", stats.entriesCount, StatTileAction.OPEN_LIST),
                Triple("Days", stats.daysCount, StatTileAction.OPEN_CALENDAR),
                Triple("Media", stats.mediaCount, StatTileAction.OPEN_MEDIA_ALL),
                Triple("On This Day", stats.onThisDayCount, StatTileAction.OPEN_TODAY),
                Triple("This week", stats.thisWeekCount, StatTileAction.OPEN_LIST),
                Triple("Place", stats.placesCount, StatTileAction.OPEN_MAP),
                Triple("Tag", stats.tagsCount, StatTileAction.OPEN_TAGS),
                Triple("Video", stats.videosCount, StatTileAction.OPEN_MEDIA_VIDEO),
                Triple("Photo", stats.photosCount, StatTileAction.OPEN_MEDIA_PHOTO),
                Triple("Audio", stats.audiosCount, StatTileAction.OPEN_MEDIA_AUDIO),
                Triple("File", stats.filesCount, StatTileAction.OPEN_MEDIA_FILE)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 5
            ) {
                tiles.forEach { (label, count, action) ->
                    StatTileCard(
                        label = label,
                        value = count.toString(),
                        onClick = { onTileClick(action) }
                    )
                }
            }
        }
    }
}

enum class StatTileAction {
    NONE,
    OPEN_LIST,
    OPEN_CALENDAR,
    OPEN_MEDIA_ALL,
    OPEN_MEDIA_PHOTO,
    OPEN_MEDIA_VIDEO,
    OPEN_MEDIA_AUDIO,
    OPEN_MEDIA_FILE,
    OPEN_MAP,
    OPEN_TAGS,
    OPEN_TODAY
}

@Composable
private fun StatTileCard(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    val palette = LocalHeyBkPalette.current
    Column(
        modifier = Modifier
            .width(104.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.cardSurface)
            .border(1.dp, palette.hairline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .testTag("stat_tile_${label.lowercase().replace(" ", "_")}"),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary,
            maxLines = 1
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = palette.textPrimary
        )
    }
}
