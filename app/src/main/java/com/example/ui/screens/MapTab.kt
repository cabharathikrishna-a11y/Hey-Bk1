package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.domain.model.JournalEntryDetail
import com.example.ui.theme.LocalHeyBkPalette
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

private data class EntryClusterItem(
    val entry: JournalEntryDetail
) : ClusterItem {
    private val latLng = LatLng(entry.latitude ?: 0.0, entry.longitude ?: 0.0)
    override fun getPosition(): LatLng = latLng
    override fun getTitle(): String = entry.title
    override fun getSnippet(): String = entry.locationName ?: entry.cityName ?: entry.bookTitle
    override fun getZIndex(): Float = 0f
}

/**
 * Matches Screenshot 6:
 * - Dark styled Google Map when MAPS_API_KEY is configured in AI Studio Secrets
 * - Teardrop cluster markers (splits dynamically city -> area -> street -> meters apart)
 * - Bottom-left teal location button
 * - Graceful dark fallback view showing location groups when MAPS_API_KEY is not yet configured
 */
@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun MapTab(
    entries: List<JournalEntryDetail>,
    focusEntry: JournalEntryDetail? = null,
    onSelectEntry: (String) -> Unit,
    onRequestLocationPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalHeyBkPalette.current
    val geoEntries = remember(entries) {
        entries.filter { it.latitude != null && it.longitude != null }
    }
    val hasMapsKey = remember {
        BuildConfig.MAPS_API_KEY.isNotBlank() &&
            BuildConfig.MAPS_API_KEY != "YOUR_GOOGLE_MAPS_API_KEY"
    }

    if (!hasMapsKey) {
        MapKeyMissingFallback(
            geoEntries = geoEntries,
            onSelectEntry = onSelectEntry,
            modifier = modifier
        )
        return
    }

    val clusterItems = remember(geoEntries) {
        geoEntries.map { EntryClusterItem(it) }
    }
    val defaultCenter = remember(geoEntries, focusEntry) {
        val target = focusEntry ?: geoEntries.firstOrNull()
        if (target?.latitude != null && target.longitude != null) {
            LatLng(target.latitude, target.longitude)
        } else {
            LatLng(16.3067, 80.4365) // Guntur default starting region
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultCenter, if (focusEntry != null) 15f else 6f)
    }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedClusterEntries by remember { mutableStateOf<List<JournalEntryDetail>>(emptyList()) }

    LaunchedEffect(Unit) {
        runCatching { MapsInitializer.initialize(context) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapStyleOptions = if (palette.isDark) MapStyleOptions(DARK_MAP_STYLE_JSON) else null
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                compassEnabled = true,
                myLocationButtonEnabled = false
            )
        ) {
            Clustering(
                items = clusterItems,
                onClusterClick = { cluster: Cluster<EntryClusterItem> ->
                    val currentZoom = cameraPositionState.position.zoom
                    if (currentZoom < 17f) {
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(cluster.position, currentZoom + 2.5f)
                            )
                        }
                    } else {
                        selectedClusterEntries = cluster.items.map { it.entry }
                    }
                    true
                },
                onClusterItemClick = { item ->
                    onSelectEntry(item.entry.id)
                    true
                },
                clusterContent = { cluster ->
                    TeardropPinBadge(
                        countText = cluster.size.toString(),
                        isHighlighted = cluster.size < 25
                    )
                },
                clusterItemContent = {
                    TeardropPinBadge(
                        countText = "1",
                        isHighlighted = true
                    )
                }
            )
        }

        // Bottom-left teal location/recenter button (matches Screenshot 6)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 20.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(palette.accentTeal)
                .clickable {
                    onRequestLocationPermission()
                    geoEntries.firstOrNull()?.let { first ->
                        if (first.latitude != null && first.longitude != null) {
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(first.latitude, first.longitude),
                                        12f
                                    )
                                )
                            }
                        }
                    }
                }
                .testTag("map_recenter_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.MyLocation,
                contentDescription = "Center map on entries",
                tint = Color.White
            )
        }

        if (selectedClusterEntries.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = palette.cardSurface
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedClusterEntries.size} Entries Here",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary
                        )
                        TextButton(onClick = { selectedClusterEntries = emptyList() }) {
                            Text("Close")
                        }
                    }
                    selectedClusterEntries.take(4).forEach { e ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedClusterEntries = emptyList()
                                    onSelectEntry(e.id)
                                }
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = e.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = e.bookTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.accentBlue
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeardropPinBadge(
    countText: String,
    isHighlighted: Boolean
) {
    val bgColor = if (isHighlighted) Color(0xFFF5B700) else Color(0xFF47505A)
    val fgColor = if (isHighlighted) Color(0xFF111214) else Color.White
    Box(
        modifier = Modifier
            .size(width = 36.dp, height = 42.dp)
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp))
            .background(bgColor)
            .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = countText,
            color = fgColor,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun MapKeyMissingFallback(
    geoEntries: List<JournalEntryDetail>,
    onSelectEntry: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalHeyBkPalette.current
    val groupedByCity = remember(geoEntries) {
        geoEntries.groupBy { it.cityName?.takeIf { c -> c.isNotBlank() } ?: "Saved Locations" }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(palette.detailPaneBackground),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.cardSurface)
                    .border(1.dp, palette.hairline, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Key, contentDescription = null, tint = palette.accentAmber)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Google Maps SDK Ready",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Add your MAPS_API_KEY in the AI Studio Secrets panel to enable interactive dark-mode Google Maps with zoom clustering (City → Area → Street pins).",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
        }

        if (geoEntries.isEmpty()) {
            item {
                Text(
                    text = "No location-tagged entries yet. New entries automatically record your location when granted permission.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else {
            groupedByCity.forEach { (city, list) ->
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = city,
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(palette.accentAmber)
                                .padding(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${list.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF111214),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                items(list, key = { it.id }) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.cardSurface)
                            .border(1.dp, palette.hairline, RoundedCornerShape(12.dp))
                            .clickable { onSelectEntry(entry.id) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = palette.accentCoral
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.title, style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
                            Text(
                                text = entry.locationName ?: "${entry.latitude}, ${entry.longitude}",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Location Picker Dialog for the Entry Editor:
 * Tap anywhere on the map to place a distinct RED arrow/pin pointing to that spot!
 */
@Composable
fun LocationPickerDialog(
    initialLat: Double?,
    initialLng: Double?,
    onDismiss: () -> Unit,
    onPickLocation: (lat: Double, lng: Double) -> Unit,
    onUseCurrentGps: () -> Unit
) {
    val palette = LocalHeyBkPalette.current
    val hasMapsKey = remember {
        BuildConfig.MAPS_API_KEY.isNotBlank() &&
            BuildConfig.MAPS_API_KEY != "YOUR_GOOGLE_MAPS_API_KEY"
    }
    var pickedLatLng by remember {
        mutableStateOf(
            LatLng(initialLat ?: 16.3067, initialLng ?: 80.4365) // Guntur default
        )
    }
    val redArrowDescriptor = remember { createRedArrowPinDescriptor() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(18.dp),
            color = palette.masterPaneBackground
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Select Place", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
                        Text(
                            text = "Tap map to point the red pin",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            onUseCurrentGps()
                            onDismiss()
                        }) {
                            Text("GPS")
                        }
                        Button(onClick = {
                            onPickLocation(pickedLatLng.latitude, pickedLatLng.longitude)
                            onDismiss()
                        }) {
                            Text("Confirm")
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (hasMapsKey) {
                        val cameraState = rememberCameraPositionState {
                            position = CameraPosition.fromLatLngZoom(pickedLatLng, 14f)
                        }
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraState,
                            properties = MapProperties(
                                mapStyleOptions = if (palette.isDark) MapStyleOptions(DARK_MAP_STYLE_JSON) else null
                            ),
                            onMapClick = { latLng -> pickedLatLng = latLng }
                        ) {
                            Marker(
                                state = MarkerState(position = pickedLatLng),
                                title = "Selected Place",
                                icon = redArrowDescriptor
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(palette.detailPaneBackground)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.LocationOn,
                                    contentDescription = null,
                                    tint = palette.accentCoral,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Add MAPS_API_KEY in AI Studio Secrets for live map picking, or tap GPS to capture your current coordinates.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            }
        }
    }
}

private fun createRedArrowPinDescriptor(): BitmapDescriptor? {
    return runCatching {
        val w = 84
        val h = 104
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#E53935")
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        val path = Path().apply {
            moveTo(w / 2f, h - 4f)
            lineTo(12f, 38f)
            arcTo(12f, 8f, w - 12f, 68f, 180f, 180f, false)
            close()
        }
        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, strokePaint)
        val innerDot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawCircle(w / 2f, 38f, 11f, innerDot)
        BitmapDescriptorFactory.fromBitmap(bmp)
    }.getOrNull()
}

private const val DARK_MAP_STYLE_JSON = """
[
  {"elementType": "geometry", "stylers": [{"color": "#141618"}]},
  {"elementType": "labels.text.fill", "stylers": [{"color": "#8f969e"}]},
  {"elementType": "labels.text.stroke", "stylers": [{"color": "#141618"}]},
  {"featureType": "administrative", "elementType": "geometry", "stylers": [{"color": "#2f343a"}]},
  {"featureType": "road", "elementType": "geometry", "stylers": [{"color": "#24282e"}]},
  {"featureType": "water", "elementType": "geometry", "stylers": [{"color": "#050607"}]}
]
"""
