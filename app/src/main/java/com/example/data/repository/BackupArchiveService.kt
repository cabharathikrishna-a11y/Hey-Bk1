package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.EntryPersonCrossRef
import com.example.data.local.EntryTagCrossRef
import com.example.data.local.EntryVersionEntity
import com.example.data.local.HeyBkDatabase
import com.example.data.local.JournalBookEntity
import com.example.data.local.JournalEntryEntity
import com.example.data.local.JournalPromptEntity
import com.example.data.local.MediaAttachmentEntity
import com.example.data.local.PersonEntity
import com.example.data.local.TagEntity
import com.example.domain.model.JournalEntryDetail
import com.example.domain.model.RichDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Serializable
data class BackupSnapshotDto(
    val version: Int = 1,
    val exportedAt: Long,
    val books: List<BackupBookDto> = emptyList(),
    val tags: List<BackupTagDto> = emptyList(),
    val people: List<BackupPersonDto> = emptyList(),
    val prompts: List<BackupPromptDto> = emptyList(),
    val entries: List<BackupEntryDto> = emptyList(),
    val media: List<BackupMediaDto> = emptyList(),
    val versions: List<BackupVersionDto> = emptyList()
)

@Serializable
data class BackupBookDto(
    val id: String,
    val title: String,
    val colorHex: String,
    val iconName: String,
    val createdAt: Long,
    val isDefault: Boolean
)

@Serializable
data class BackupTagDto(val id: String, val name: String)

@Serializable
data class BackupPersonDto(
    val id: String,
    val displayName: String,
    val contactLookupUri: String? = null,
    val note: String = ""
)

@Serializable
data class BackupPromptDto(
    val id: String,
    val category: String,
    val promptText: String,
    val isCustom: Boolean,
    val isFavorite: Boolean
)

@Serializable
data class BackupEntryDto(
    val id: String,
    val bookId: String,
    val title: String,
    val richContentJson: String,
    val plainTextCache: String,
    val entryTimestamp: Long,
    val updatedAt: Long,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
    val cityName: String? = null,
    val areaName: String? = null,
    val streetName: String? = null,
    val isFavorite: Boolean = false,
    val isTrashed: Boolean = false,
    val trashedAt: Long? = null,
    val tagIds: List<String> = emptyList(),
    val personIds: List<String> = emptyList()
)

@Serializable
data class BackupMediaDto(
    val id: String,
    val entryId: String,
    val type: String,
    val archivePath: String,
    val displayName: String,
    val durationMs: Long,
    val createdAt: Long
)

@Serializable
data class BackupVersionDto(
    val id: String,
    val entryId: String,
    val titleSnapshot: String,
    val richContentJson: String,
    val savedAt: Long,
    val summaryLabel: String
)

data class ImportResultSummary(
    val entriesImported: Int,
    val booksImported: Int,
    val mediaRestored: Int
)

class BackupArchiveService(
    private val context: Context,
    private val db: HeyBkDatabase
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportToZipUri(targetUri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val dao = db.journalDao()
            val books = dao.getAllBooksOnce()
            val tags = dao.getAllTagsOnce()
            val people = dao.getAllPeopleOnce()
            val prompts = dao.getAllPromptsOnce()
            val entriesWithRel = dao.getAllEntriesOnce()
            val versions = dao.getAllVersionsOnce()

            val mediaDtos = mutableListOf<BackupMediaDto>()
            val mediaFilesToPack = mutableListOf<Pair<String, File>>()

            entriesWithRel.forEach { rel ->
                rel.media.forEach { m ->
                    val sourceFile = resolveLocalFile(m.localUri)
                    val entryPath = "media/${m.id}_${m.displayName.replace("/", "_")}"
                    if (sourceFile != null && sourceFile.exists()) {
                        mediaFilesToPack.add(entryPath to sourceFile)
                    }
                    mediaDtos.add(
                        BackupMediaDto(
                            id = m.id,
                            entryId = m.entryId,
                            type = m.type,
                            archivePath = if (sourceFile != null && sourceFile.exists()) entryPath else m.localUri,
                            displayName = m.displayName,
                            durationMs = m.durationMs,
                            createdAt = m.createdAt
                        )
                    )
                }
            }

            val snapshot = BackupSnapshotDto(
                exportedAt = System.currentTimeMillis(),
                books = books.map { BackupBookDto(it.id, it.title, it.colorHex, it.iconName, it.createdAt, it.isDefault) },
                tags = tags.map { BackupTagDto(it.id, it.name) },
                people = people.map { BackupPersonDto(it.id, it.displayName, it.contactLookupUri, it.note) },
                prompts = prompts.map { BackupPromptDto(it.id, it.category, it.promptText, it.isCustom, it.isFavorite) },
                entries = entriesWithRel.map { rel ->
                    val e = rel.entry
                    BackupEntryDto(
                        id = e.id,
                        bookId = e.bookId,
                        title = e.title,
                        richContentJson = e.richContentJson,
                        plainTextCache = e.plainTextCache,
                        entryTimestamp = e.entryTimestamp,
                        updatedAt = e.updatedAt,
                        latitude = e.latitude,
                        longitude = e.longitude,
                        locationName = e.locationName,
                        cityName = e.cityName,
                        areaName = e.areaName,
                        streetName = e.streetName,
                        isFavorite = e.isFavorite,
                        isTrashed = e.isTrashed,
                        trashedAt = e.trashedAt,
                        tagIds = rel.tags.map { it.id },
                        personIds = rel.people.map { it.id }
                    )
                },
                media = mediaDtos,
                versions = versions.map {
                    BackupVersionDto(it.id, it.entryId, it.titleSnapshot, it.richContentJson, it.savedAt, it.summaryLabel)
                }
            )

            val outStream = context.contentResolver.openOutputStream(targetUri)
                ?: error("Unable to open output stream for backup")
            ZipOutputStream(outStream).use { zip ->
                zip.putNextEntry(ZipEntry("hey_bk_backup.json"))
                zip.write(json.encodeToString(snapshot).toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                mediaFilesToPack.distinctBy { it.first }.forEach { (archivePath, file) ->
                    zip.putNextEntry(ZipEntry(archivePath))
                    FileInputStream(file).use { input -> input.copyTo(zip) }
                    zip.closeEntry()
                }
            }
            entriesWithRel.size
        }
    }

    suspend fun importFromUri(sourceUri: Uri): Result<ImportResultSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val mediaDir = File(context.filesDir, "media").apply { mkdirs() }
            var snapshotJson: String? = null
            val extractedMediaMap = mutableMapOf<String, String>()

            val rawBytes = context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
                ?: error("Unable to open backup file")

            val isZip = rawBytes.size >= 2 && rawBytes[0] == 0x50.toByte() && rawBytes[1] == 0x4B.toByte()
            if (isZip) {
                ZipInputStream(rawBytes.inputStream()).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            if (entry.name == "hey_bk_backup.json") {
                                snapshotJson = zip.readBytes().toString(Charsets.UTF_8)
                            } else if (entry.name.startsWith("media/")) {
                                val outFile = File(mediaDir, entry.name.removePrefix("media/").replace("/", "_"))
                                FileOutputStream(outFile).use { out -> zip.copyTo(out) }
                                extractedMediaMap[entry.name] = Uri.fromFile(outFile).toString()
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } else {
                snapshotJson = rawBytes.toString(Charsets.UTF_8)
            }

            val parsed = json.decodeFromString<BackupSnapshotDto>(
                snapshotJson ?: error("No hey_bk_backup.json found in archive")
            )

            val books = parsed.books.map {
                JournalBookEntity(it.id, it.title, it.colorHex, it.iconName, it.createdAt, it.isDefault)
            }
            val tags = parsed.tags.map { TagEntity(it.id, it.name) }
            val people = parsed.people.map { PersonEntity(it.id, it.displayName, it.contactLookupUri, it.note) }
            val prompts = parsed.prompts.map {
                JournalPromptEntity(it.id, it.category, it.promptText, it.isCustom, it.isFavorite)
            }
            val entries = parsed.entries.map {
                JournalEntryEntity(
                    id = it.id,
                    bookId = it.bookId,
                    title = it.title,
                    richContentJson = it.richContentJson,
                    plainTextCache = it.plainTextCache,
                    entryTimestamp = it.entryTimestamp,
                    updatedAt = it.updatedAt,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    locationName = it.locationName,
                    cityName = it.cityName,
                    areaName = it.areaName,
                    streetName = it.streetName,
                    isFavorite = it.isFavorite,
                    isTrashed = it.isTrashed,
                    trashedAt = it.trashedAt
                )
            }
            val mediaEntities = parsed.media.map { m ->
                val resolvedUri = extractedMediaMap[m.archivePath] ?: m.archivePath
                MediaAttachmentEntity(
                    id = m.id,
                    entryId = m.entryId,
                    type = m.type,
                    localUri = resolvedUri,
                    displayName = m.displayName,
                    durationMs = m.durationMs,
                    createdAt = m.createdAt
                )
            }
            val tagRefs = parsed.entries.flatMap { e -> e.tagIds.map { tid -> EntryTagCrossRef(e.id, tid) } }
            val personRefs = parsed.entries.flatMap { e -> e.personIds.map { pid -> EntryPersonCrossRef(e.id, pid) } }
            val versions = parsed.versions.map {
                EntryVersionEntity(it.id, it.entryId, it.titleSnapshot, it.richContentJson, it.savedAt, it.summaryLabel)
            }

            db.journalDao().importFullSnapshot(
                books = books,
                tags = tags,
                people = people,
                prompts = prompts,
                entries = entries,
                media = mediaEntities,
                tagRefs = tagRefs,
                personRefs = personRefs,
                versions = versions
            )

            ImportResultSummary(
                entriesImported = entries.size,
                booksImported = books.size,
                mediaRestored = extractedMediaMap.size
            )
        }
    }

    fun formatSingleEntryAsMarkdown(entry: JournalEntryDetail): String {
        val dateFmt = SimpleDateFormat("EEE, dd MMM yyyy • h:mm a", Locale.getDefault())
        val doc = RichDocument.decode(entry.richContentJson)
        return buildString {
            appendLine("# ${entry.title}")
            appendLine()
            appendLine("**Book:** ${entry.bookTitle}  ")
            appendLine("**Date:** ${dateFmt.format(Date(entry.entryTimestamp))}  ")
            if (!entry.locationName.isNullOrBlank()) {
                appendLine("**Location:** ${entry.locationName}  ")
            }
            if (entry.people.isNotEmpty()) {
                appendLine("**People:** ${entry.people.joinToString(", ") { it.displayName }}  ")
            }
            if (entry.tags.isNotEmpty()) {
                appendLine("**Tags:** ${entry.tags.joinToString(", ") { "#${it.name}" }}  ")
            }
            appendLine()
            appendLine("---")
            appendLine()
            appendLine(doc.toMarkdown())
        }
    }

    private fun resolveLocalFile(uriString: String): File? {
        return try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file" && uri.path != null) File(uri.path!!) else null
        } catch (_: Exception) {
            null
        }
    }
}
