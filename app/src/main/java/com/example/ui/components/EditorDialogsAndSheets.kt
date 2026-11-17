package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.example.domain.model.DefaultTemplates
import com.example.domain.model.EntryTemplate
import com.example.domain.model.EntryVersion
import com.example.domain.model.JournalBook
import com.example.domain.model.Person
import com.example.domain.model.PersonSummary
import com.example.domain.model.RichDocument
import com.example.domain.model.Tag
import com.example.ui.theme.LocalHeyBkPalette
import com.example.ui.theme.parseColorHex
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagAndPeopleSheet(
    currentTags: List<String>,
    allTags: List<Tag>,
    currentPeople: List<Person>,
    allPeople: List<PersonSummary>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onPickFromContacts: () -> Unit,
    onAddOrAttachPerson: (String) -> Unit,
    onRemovePerson: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalHeyBkPalette.current
    var newTagText by remember { mutableStateOf("") }
    var newPersonText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.masterPaneBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Tags & People", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)

            // Tags section
            Text("TAGS", style = MaterialTheme.typography.labelSmall, color = palette.textMuted)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newTagText,
                    onValueChange = { newTagText = it },
                    label = { Text("Add tag (e.g. travel, study)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("tag_input_field")
                )
                Button(
                    onClick = {
                        onAddTag(newTagText)
                        newTagText = ""
                    },
                    enabled = newTagText.isNotBlank(),
                    modifier = Modifier.testTag("add_tag_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add tag")
                }
            }

            if (currentTags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currentTags.forEach { tagName ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(palette.selectedCard)
                                .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Label, null, tint = palette.accentBlue, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("#$tagName", style = MaterialTheme.typography.labelMedium, color = palette.textPrimary)
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Remove tag $tagName",
                                tint = palette.textSecondary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onRemoveTag(tagName) }
                            )
                        }
                    }
                }
            }

            val suggestedTags = allTags.filter { t -> currentTags.none { it.equals(t.name, ignoreCase = true) } }
            if (suggestedTags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestedTags.take(10).forEach { tag ->
                        Text(
                            text = "+ #${tag.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(palette.cardSurface)
                                .border(1.dp, palette.hairline, RoundedCornerShape(16.dp))
                                .clickable { onAddTag(tag.name) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = palette.hairline)

            // People / Contacts section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("PEOPLE & CONTACTS", style = MaterialTheme.typography.labelSmall, color = palette.textMuted)
                OutlinedButton(
                    onClick = onPickFromContacts,
                    modifier = Modifier.testTag("pick_contact_button")
                ) {
                    Icon(Icons.Outlined.Contacts, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Pick from Contacts")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newPersonText,
                    onValueChange = { newPersonText = it },
                    label = { Text("Or add custom person name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("custom_person_input")
                )
                Button(
                    onClick = {
                        onAddOrAttachPerson(newPersonText)
                        newPersonText = ""
                    },
                    enabled = newPersonText.isNotBlank(),
                    modifier = Modifier.testTag("add_custom_person_button")
                ) {
                    Icon(Icons.Outlined.PersonAdd, contentDescription = "Add person")
                }
            }

            if (currentPeople.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currentPeople.forEach { p ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(palette.accentAmber.copy(alpha = 0.16f))
                                .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("@${p.displayName}", style = MaterialTheme.typography.labelMedium, color = palette.accentAmber)
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Remove ${p.displayName}",
                                tint = palette.accentAmber,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onRemovePerson(p.id) }
                            )
                        }
                    }
                }
            }

            val suggestedPeople = allPeople.filter { ps -> currentPeople.none { it.id == ps.person.id } }
            if (suggestedPeople.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestedPeople.take(10).forEach { ps ->
                        Text(
                            text = "+ @${ps.person.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(palette.cardSurface)
                                .border(1.dp, palette.hairline, RoundedCornerShape(16.dp))
                                .clickable { onAddOrAttachPerson(ps.person.displayName) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesBottomSheet(
    onSelectTemplate: (EntryTemplate) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalHeyBkPalette.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.masterPaneBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Templates", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
            DefaultTemplates.all.forEach { tpl ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.cardSurface)
                        .border(1.dp, palette.hairline, RoundedCornerShape(14.dp))
                        .clickable {
                            onSelectTemplate(tpl)
                            onDismiss()
                        }
                        .padding(14.dp)
                ) {
                    Text(tpl.name, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                    Spacer(Modifier.height(2.dp))
                    Text(tpl.description, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionHistoryBottomSheet(
    versions: List<EntryVersion>,
    onRestoreVersion: (EntryVersion) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalHeyBkPalette.current
    val fmt = remember { SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.masterPaneBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.History, null, tint = palette.accentBlue)
                Spacer(Modifier.width(8.dp))
                Text("Version History", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
            }
            Spacer(Modifier.height(12.dp))
            if (versions.isEmpty()) {
                Text(
                    text = "Save this entry to start tracking version snapshots.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(versions, key = { it.id }) { version ->
                        val preview = remember(version.richContentJson) {
                            RichDocument.decode(version.richContentJson).toPlainText().take(120)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.cardSurface)
                                .border(1.dp, palette.hairline, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = fmt.format(Date(version.savedAt)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.accentBlue
                                )
                                Text(
                                    text = version.titleSnapshot,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = palette.textPrimary
                                )
                                if (preview.isNotBlank()) {
                                    Text(
                                        text = preview,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary,
                                        maxLines = 2
                                    )
                                }
                            }
                            TextButton(onClick = {
                                onRestoreVersion(version)
                                onDismiss()
                            }) {
                                Text("Restore")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
fun BookPickerDialog(
    title: String,
    books: List<JournalBook>,
    currentBookId: String?,
    onSelectBook: (JournalBook) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalHeyBkPalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                books.forEach { book ->
                    val isCurrent = book.id == currentBookId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCurrent) palette.selectedCard else palette.cardSurface)
                            .clickable {
                                onSelectBook(book)
                                onDismiss()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BookCoverGlyph(color = parseColorHex(book.colorHex))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = palette.textPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun EntryInfoDialog(
    title: String,
    bookTitle: String,
    document: RichDocument,
    createdTimestamp: Long,
    updatedTimestamp: Long,
    locationName: String?,
    latitude: Double?,
    longitude: Double?,
    mediaCount: Int,
    onDismiss: () -> Unit
) {
    val fmt = remember { SimpleDateFormat("EEE, dd MMM yyyy • h:mm a", Locale.getDefault()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Entry Info") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Title: ${title.ifBlank { "Untitled" }}")
                Text("Book: $bookTitle")
                Text("Words: ${document.wordCount()} • Characters: ${document.charCount()}")
                Text("Media attachments: $mediaCount")
                Text("Created: ${fmt.format(Date(createdTimestamp))}")
                Text("Updated: ${fmt.format(Date(updatedTimestamp))}")
                if (!locationName.isNullOrBlank()) {
                    Text("Place: $locationName")
                }
                if (latitude != null && longitude != null) {
                    Text("Coordinates: ${"%.5f".format(latitude)}, ${"%.5f".format(longitude)}")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
fun HyperlinkDialog(
    onDismiss: () -> Unit,
    onConfirm: (url: String) -> Unit
) {
    var url by remember { mutableStateOf("https://") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Hyperlink") },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(url.trim())
                    onDismiss()
                },
                enabled = url.isNotBlank()
            ) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AiComingSoonDialog(onDismiss: () -> Unit) {
    val palette = LocalHeyBkPalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = palette.accentBlue) },
        title = { Text("AI Assistant Slot Ready") },
        text = {
            Text(
                "Hey Bk's AI slot is wired and waiting for your Gemini workflow instructions in the next step—such as smart reflections, entry summaries, or daily chat."
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Got it") }
        }
    )
}
