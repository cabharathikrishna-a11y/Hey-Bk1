package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.JournalEntryDetail
import com.example.domain.model.JournalPrompt
import com.example.domain.model.PersonSummary
import com.example.domain.model.PromptCategory
import com.example.domain.model.Tag
import com.example.system.MediaStorageHelper
import com.example.ui.theme.LocalHeyBkPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PromptsScreen(
    prompts: List<JournalPrompt>,
    onStartEntryWithPrompt: (JournalPrompt) -> Unit,
    onToggleFavorite: (JournalPrompt) -> Unit,
    onAddCustomPrompt: (category: String, text: String) -> Unit,
    onDeleteCustomPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalHeyBkPalette.current
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filtered = remember(prompts, selectedCategory) {
        if (selectedCategory == null) prompts else prompts.filter { it.category == selectedCategory }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.masterPaneBackground)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Prompts", style = MaterialTheme.typography.headlineLarge, color = palette.textPrimary)
                Text("Prompts Bank across 9 themes", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
            }
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_custom_prompt_button")
            ) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(4.dp))
                Text("Add")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PromptCategoryChip("All", selectedCategory == null) { selectedCategory = null }
            PromptCategory.entries.forEach { cat ->
                PromptCategoryChip(cat.title, selectedCategory == cat.title) {
                    selectedCategory = cat.title
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered, key = { it.id }) { prompt ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.cardSurface)
                        .border(1.dp, palette.hairline, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = prompt.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.accentBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row {
                            IconButton(onClick = { onToggleFavorite(prompt) }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    if (prompt.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                    contentDescription = "Favorite prompt",
                                    tint = if (prompt.isFavorite) palette.accentAmber else palette.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            if (prompt.isCustom) {
                                IconButton(
                                    onClick = { onDeleteCustomPrompt(prompt.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.DeleteOutline,
                                        contentDescription = "Delete prompt",
                                        tint = palette.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Outlined.FormatQuote,
                            contentDescription = null,
                            tint = palette.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = prompt.promptText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.textPrimary
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { onStartEntryWithPrompt(prompt) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Outlined.EditNote, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Write Entry")
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var cat by remember { mutableStateOf(PromptCategory.REFLECTION.title) }
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Prompt to Bank") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PromptCategory.entries.forEach { c ->
                            PromptCategoryChip(c.title, cat == c.title) { cat = c.title }
                        }
                    }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Prompt question") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAddCustomPrompt(cat, text)
                        showAddDialog = false
                    },
                    enabled = text.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PromptCategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalHeyBkPalette.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) palette.selectedCard else palette.cardSurface)
            .border(1.dp, if (selected) palette.accentBlue else palette.hairline, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) palette.accentBlue else palette.textSecondary
        )
    }
}

@Composable
fun PeopleDirectoryScreen(
    peopleSummaries: List<PersonSummary>,
    mediaHelper: MediaStorageHelper,
    onAddPerson: (name: String, contactUri: String?, note: String) -> Unit,
    onDeletePerson: (String) -> Unit,
    onSelectPersonToFilter: (String) -> Unit,
    onPostMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalHeyBkPalette.current
    val fmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    var search by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri != null) {
            val resolved = mediaHelper.resolveContactSelection(uri)
            if (resolved != null) {
                onAddPerson(resolved.first, resolved.second, "")
            } else {
                onPostMessage("Could not read contact name")
            }
        }
    }

    val filtered = remember(peopleSummaries, search) {
        if (search.isBlank()) peopleSummaries
        else peopleSummaries.filter { it.person.displayName.contains(search, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.masterPaneBackground)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("People", style = MaterialTheme.typography.headlineLarge, color = palette.textPrimary)
                Text("Tap anyone to see every shared memory", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { contactPickerLauncher.launch(null) },
                    modifier = Modifier.testTag("people_pick_contact")
                ) {
                    Icon(Icons.Outlined.Contacts, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Contacts")
                }
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("people_add_custom")
                ) {
                    Icon(Icons.Outlined.PersonAdd, null, modifier = Modifier.size(16.dp))
                }
            }
        }

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Search person (e.g. When did we talk last?)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No people tagged yet. Pick from Contacts or add someone custom.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.person.id }) { summary ->
                    val lastSeen = summary.lastInteractedAt?.let { fmt.format(Date(it)) } ?: "No entries yet"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.cardSurface)
                            .border(1.dp, palette.hairline, RoundedCornerShape(14.dp))
                            .clickable { onSelectPersonToFilter(summary.person.id) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(palette.accentAmber.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = summary.person.displayName.take(2).uppercase(),
                                color = palette.accentAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(summary.person.displayName, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                            Text(
                                text = "Last mentioned: $lastSeen • ${summary.entriesCount} entries",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        IconButton(onClick = { onDeletePerson(summary.person.id) }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete person", tint = palette.textMuted)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Person") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Relationship / note (optional)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAddPerson(name, null, note)
                        showAddDialog = false
                    },
                    enabled = name.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsDirectoryScreen(
    tags: List<Tag>,
    entries: List<JournalEntryDetail>,
    onSelectTagToFilter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalHeyBkPalette.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.masterPaneBackground)
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        Text("Tags (${tags.size})", style = MaterialTheme.typography.headlineLarge, color = palette.textPrimary)
        Spacer(Modifier.height(6.dp))
        Text("Tap any tag to view all matching entries", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
        Spacer(Modifier.height(16.dp))

        if (tags.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tags yet. Add tags inside any journal entry.", color = palette.textSecondary)
            }
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                tags.forEach { tag ->
                    val count = entries.count { e -> e.tags.any { it.id == tag.id } }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(palette.cardSurface)
                            .border(1.dp, palette.hairline, RoundedCornerShape(20.dp))
                            .clickable { onSelectTagToFilter(tag.id) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Label, null, tint = palette.accentBlue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("#${tag.name} ($count)", style = MaterialTheme.typography.labelLarge, color = palette.textPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun TrashScreen(
    trashedEntries: List<JournalEntryDetail>,
    onRestore: (String) -> Unit,
    onDeletePermanently: (String) -> Unit,
    onEmptyTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalHeyBkPalette.current
    val fmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.masterPaneBackground)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Trash", style = MaterialTheme.typography.headlineLarge, color = palette.textPrimary)
            if (trashedEntries.isNotEmpty()) {
                TextButton(onClick = onEmptyTrash) {
                    Text("Empty Trash", color = palette.accentCoral)
                }
            }
        }

        if (trashedEntries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Trash is empty", style = MaterialTheme.typography.bodyLarge, color = palette.textSecondary)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(trashedEntries, key = { it.id }) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.cardSurface)
                            .border(1.dp, palette.hairline, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.title, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                            Text(
                                text = "${entry.bookTitle} • ${fmt.format(Date(entry.entryTimestamp))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        IconButton(onClick = { onRestore(entry.id) }) {
                            Icon(Icons.Outlined.RestoreFromTrash, "Restore", tint = palette.accentBlue)
                        }
                        IconButton(onClick = { onDeletePermanently(entry.id) }) {
                            Icon(Icons.Outlined.DeleteOutline, "Delete permanently", tint = palette.accentCoral)
                        }
                    }
                }
            }
        }
    }
}
