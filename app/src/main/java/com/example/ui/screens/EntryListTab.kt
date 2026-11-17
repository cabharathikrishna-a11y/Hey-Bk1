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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.JournalEntryDetail
import com.example.domain.model.MediaType
import com.example.ui.state.dayKeyOf
import com.example.ui.theme.LocalHeyBkPalette
import com.example.ui.theme.parseColorHex
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EntryListTab(
    entries: List<JournalEntryDetail>,
    selectedEntryId: String?,
    onSelectEntry: (String) -> Unit,
    onCreateNewEntry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalHeyBkPalette.current

    if (entries.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.AutoStories,
                    contentDescription = null,
                    tint = palette.accentBlue,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Your journal begins here",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Tap + New entry to capture a thought, photo, person, or place.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onCreateNewEntry,
                    modifier = Modifier.testTag("empty_list_new_entry")
                ) {
                    Text("Write First Entry")
                }
            }
        }
        return
    }

    val monthFmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val dayOfWeekFmt = SimpleDateFormat("EEE", Locale.getDefault())
    val dayNumFmt = SimpleDateFormat("dd", Locale.getDefault())
    val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(entries, key = { _, item -> item.id }) { index, entry ->
            val currentMonth = monthFmt.format(Date(entry.entryTimestamp))
            val prevMonth = if (index > 0) monthFmt.format(Date(entries[index - 1].entryTimestamp)) else null
            if (currentMonth != prevMonth) {
                Text(
                    text = currentMonth,
                    style = MaterialTheme.typography.headlineSmall,
                    color = palette.textPrimary,
                    modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 4.dp)
                )
            }

            val showDayBadge = index == 0 ||
                dayKeyOf(entries[index - 1].entryTimestamp) != dayKeyOf(entry.entryTimestamp)
            val isSelected = entry.id == selectedEntryId
            val cardBg = if (isSelected) palette.selectedCard else palette.cardSurface
            val bookColor = parseColorHex(entry.bookColorHex, palette.accentAmber)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardBg)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) palette.accentBlue.copy(alpha = 0.45f) else palette.hairline,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelectEntry(entry.id) }
                    .padding(12.dp)
                    .testTag("entry_card_${entry.id}"),
                verticalAlignment = Alignment.Top
            ) {
                // Left circular day pill (matches Screenshot 1 & 2)
                Box(
                    modifier = Modifier.width(50.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (showDayBadge) {
                        Column(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(palette.elevatedTile),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = dayOfWeekFmt.format(Date(entry.entryTimestamp)).uppercase(),
                                fontSize = 9.sp,
                                color = palette.textSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = dayNumFmt.format(Date(entry.entryTimestamp)),
                                fontSize = 16.sp,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (entry.isFavorite) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = "Favorite",
                                tint = palette.accentAmber,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    if (entry.plainTextCache.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = entry.plainTextCache.replace("\n", " "),
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = entry.bookTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = bookColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "• ${timeFmt.format(Date(entry.entryTimestamp))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textMuted
                        )
                        if (!entry.cityName.isNullOrBlank() || !entry.locationName.isNullOrBlank()) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = "Has location",
                                tint = palette.textMuted,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        if (entry.people.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Outlined.PersonOutline,
                                contentDescription = "Has tagged people",
                                tint = palette.accentBlue,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                val thumbPhoto = entry.firstPhoto
                if (thumbPhoto != null) {
                    Spacer(Modifier.width(10.dp))
                    AsyncImage(
                        model = thumbPhoto.localUri,
                        contentDescription = "Entry photo thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else if (entry.media.isNotEmpty()) {
                    Spacer(Modifier.width(10.dp))
                    val firstOther = entry.media.first()
                    val mediaIcon = when (firstOther.type) {
                        MediaType.VIDEO -> Icons.Outlined.Videocam
                        MediaType.AUDIO -> Icons.Outlined.GraphicEq
                        else -> Icons.Outlined.AttachFile
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.elevatedTile),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(mediaIcon, contentDescription = null, tint = palette.textSecondary)
                    }
                }
            }
        }
    }
}
