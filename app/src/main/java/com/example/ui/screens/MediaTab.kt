package com.example.ui.screens

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import com.example.domain.model.JournalEntryDetail
import com.example.domain.model.MediaAttachment
import com.example.domain.model.MediaFilter
import com.example.domain.model.MediaType
import com.example.ui.theme.LocalHeyBkPalette
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class MediaGalleryItem {
    abstract val entry: JournalEntryDetail
    abstract val timestamp: Long

    data class AttachmentItem(
        override val entry: JournalEntryDetail,
        val attachment: MediaAttachment
    ) : MediaGalleryItem() {
        override val timestamp: Long get() = entry.entryTimestamp
    }

    data class MapLocationItem(
        override val entry: JournalEntryDetail
    ) : MediaGalleryItem() {
        override val timestamp: Long get() = entry.entryTimestamp
    }
}

/**
 * Matches Screenshot 5:
 * - Filter chips: All, Photos, Video, Audio, Files, Maps
 * - Large rounded media cards with day number + Month Year in bottom-left corner
 * - Tapping photo opens clean pinch-to-zoom full-screen viewer WITHOUT date overlay
 */
@Composable
fun MediaTab(
    entries: List<JournalEntryDetail>,
    selectedFilter: MediaFilter,
    onSelectFilter: (MediaFilter) -> Unit,
    onOpenEntry: (String) -> Unit,
    onJumpToMapForEntry: (JournalEntryDetail) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalHeyBkPalette.current
    val context = LocalContext.current
    val videoImageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
    }

    var zoomedPhotoUri by remember { mutableStateOf<String?>(null) }
    var playingVideoUri by remember { mutableStateOf<String?>(null) }
    var playingAudio by remember { mutableStateOf<MediaAttachment?>(null) }

    val items = remember(entries, selectedFilter) {
        val list = mutableListOf<MediaGalleryItem>()
        entries.forEach { entry ->
            when (selectedFilter) {
                MediaFilter.ALL -> {
                    entry.media.forEach { list.add(MediaGalleryItem.AttachmentItem(entry, it)) }
                    if (entry.latitude != null && entry.longitude != null && entry.media.isEmpty()) {
                        list.add(MediaGalleryItem.MapLocationItem(entry))
                    }
                }
                MediaFilter.PHOTOS -> entry.media
                    .filter { it.type == MediaType.PHOTO }
                    .forEach { list.add(MediaGalleryItem.AttachmentItem(entry, it)) }
                MediaFilter.VIDEO -> entry.media
                    .filter { it.type == MediaType.VIDEO }
                    .forEach { list.add(MediaGalleryItem.AttachmentItem(entry, it)) }
                MediaFilter.AUDIO -> entry.media
                    .filter { it.type == MediaType.AUDIO }
                    .forEach { list.add(MediaGalleryItem.AttachmentItem(entry, it)) }
                MediaFilter.FILES -> entry.media
                    .filter { it.type == MediaType.DOCUMENT }
                    .forEach { list.add(MediaGalleryItem.AttachmentItem(entry, it)) }
                MediaFilter.MAPS -> {
                    if (entry.latitude != null && entry.longitude != null) {
                        list.add(MediaGalleryItem.MapLocationItem(entry))
                    }
                }
            }
        }
        list.sortedByDescending { it.timestamp }
    }

    val dayFmt = remember { SimpleDateFormat("dd", Locale.getDefault()) }
    val monthYearFmt = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MediaFilterChip("All", Icons.Outlined.PhotoLibrary, selectedFilter == MediaFilter.ALL) {
                onSelectFilter(MediaFilter.ALL)
            }
            MediaFilterChip("Photos", Icons.Outlined.Image, selectedFilter == MediaFilter.PHOTOS) {
                onSelectFilter(MediaFilter.PHOTOS)
            }
            MediaFilterChip("Video", Icons.Outlined.Videocam, selectedFilter == MediaFilter.VIDEO) {
                onSelectFilter(MediaFilter.VIDEO)
            }
            MediaFilterChip("Audio", Icons.Outlined.GraphicEq, selectedFilter == MediaFilter.AUDIO) {
                onSelectFilter(MediaFilter.AUDIO)
            }
            MediaFilterChip("Files", Icons.Outlined.Description, selectedFilter == MediaFilter.FILES) {
                onSelectFilter(MediaFilter.FILES)
            }
            MediaFilterChip("Maps", Icons.Outlined.LocationOn, selectedFilter == MediaFilter.MAPS) {
                onSelectFilter(MediaFilter.MAPS)
            }
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No ${selectedFilter.label.lowercase()} media attached yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.textSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items,
                    key = {
                        when (it) {
                            is MediaGalleryItem.AttachmentItem -> it.attachment.id
                            is MediaGalleryItem.MapLocationItem -> "map_${it.entry.id}"
                        }
                    }
                ) { item ->
                    val date = Date(item.timestamp)
                    val dayText = dayFmt.format(date).trimStart('0').ifEmpty { "0" }
                    val monthYearText = monthYearFmt.format(date)

                    when (item) {
                        is MediaGalleryItem.AttachmentItem -> {
                            val att = item.attachment
                            when (att.type) {
                                MediaType.PHOTO -> {
                                    MediaVisualCard(
                                        dayText = dayText,
                                        monthYearText = monthYearText,
                                        onClick = { zoomedPhotoUri = att.localUri }
                                    ) {
                                        AsyncImage(
                                            model = att.localUri,
                                            contentDescription = att.displayName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                MediaType.VIDEO -> {
                                    MediaVisualCard(
                                        dayText = dayText,
                                        monthYearText = monthYearText,
                                        onClick = { playingVideoUri = att.localUri }
                                    ) {
                                        AsyncImage(
                                            model = att.localUri,
                                            imageLoader = videoImageLoader,
                                            contentDescription = att.displayName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.55f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.PlayCircleOutline,
                                                contentDescription = "Play video",
                                                tint = Color.White,
                                                modifier = Modifier.size(34.dp)
                                            )
                                        }
                                    }
                                }
                                MediaType.AUDIO -> {
                                    MediaInfoCard(
                                        icon = Icons.Outlined.GraphicEq,
                                        title = att.displayName,
                                        subtitle = "${item.entry.title} • $dayText $monthYearText",
                                        accent = palette.accentBlue,
                                        onClick = { playingAudio = att }
                                    )
                                }
                                MediaType.DOCUMENT -> {
                                    MediaInfoCard(
                                        icon = Icons.Outlined.Description,
                                        title = att.displayName,
                                        subtitle = "${item.entry.title} • $dayText $monthYearText",
                                        accent = palette.accentAmber,
                                        onClick = {
                                            openDocumentExternally(context, att.localUri) {
                                                onOpenEntry(item.entry.id)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        is MediaGalleryItem.MapLocationItem -> {
                            MediaInfoCard(
                                icon = Icons.Outlined.LocationOn,
                                title = item.entry.locationName ?: "Pinned Location",
                                subtitle = "${item.entry.title} • $dayText $monthYearText",
                                accent = palette.accentCoral,
                                onClick = { onJumpToMapForEntry(item.entry) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Clean full-screen pinch-to-zoom photo viewer (NO date overlay)
    if (zoomedPhotoUri != null) {
        CleanPhotoZoomDialog(
            uri = zoomedPhotoUri!!,
            onDismiss = { zoomedPhotoUri = null }
        )
    }

    if (playingVideoUri != null) {
        VideoPlayerDialog(
            uri = playingVideoUri!!,
            onDismiss = { playingVideoUri = null }
        )
    }

    if (playingAudio != null) {
        AudioPlayerDialog(
            attachment = playingAudio!!,
            onDismiss = { playingAudio = null }
        )
    }
}

@Composable
private fun MediaFilterChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalHeyBkPalette.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) palette.selectedCard else palette.cardSurface)
            .border(
                1.dp,
                if (selected) palette.accentBlue else palette.hairline,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("media_filter_${label.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) palette.accentBlue else palette.textSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) palette.textPrimary else palette.textSecondary
        )
    }
}

@Composable
private fun MediaVisualCard(
    dayText: String,
    monthYearText: String,
    onClick: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.05f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        content()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
        ) {
            Text(
                text = dayText,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = monthYearText,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun MediaInfoCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit
) {
    val palette = LocalHeyBkPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.cardSurface)
            .border(1.dp, palette.hairline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CleanPhotoZoomDialog(
    uri: String,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val nextScale = (scale * zoom).coerceIn(1f, 5f)
                        scale = nextScale
                        offset = if (nextScale > 1f) offset + pan else Offset.Zero
                    }
                }
        ) {
            AsyncImage(
                model = uri,
                contentDescription = "Zoomed journal photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .testTag("close_photo_zoom")
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Close zoom view", tint = Color.White)
            }
        }
    }
}

@Composable
fun VideoPlayerDialog(uri: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(Uri.parse(uri))
                        val controller = MediaController(ctx)
                        controller.setAnchorView(this)
                        setMediaController(controller)
                        start()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Close video", tint = Color.White)
            }
        }
    }
}

@Composable
fun AudioPlayerDialog(attachment: MediaAttachment, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    val player = remember(attachment.localUri) {
        MediaPlayer().apply {
            runCatching {
                setDataSource(context, Uri.parse(attachment.localUri))
                prepare()
                setOnCompletionListener { isPlaying = false }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                if (player.isPlaying) player.stop()
                player.release()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(attachment.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = {
                    if (player.isPlaying) {
                        player.pause()
                        isPlaying = false
                    } else {
                        player.start()
                        isPlaying = true
                    }
                }) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (isPlaying) "Pause" else "Play Audio")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

private fun openDocumentExternally(
    context: android.content.Context,
    localUri: String,
    fallback: () -> Unit
) {
    runCatching {
        val parsed = Uri.parse(localUri)
        val contentUri = if (parsed.scheme == "file" && parsed.path != null) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(parsed.path!!)
            )
        } else parsed
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open document"))
    }.onFailure { fallback() }
}
