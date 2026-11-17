package com.example.data.repository

import com.example.data.local.EntryVersionEntity
import com.example.data.local.JournalBookEntity
import com.example.data.local.JournalDao
import com.example.data.local.JournalEntryEntity
import com.example.data.local.JournalEntryWithRelations
import com.example.data.local.JournalPromptEntity
import com.example.data.local.MediaAttachmentEntity
import com.example.data.local.PersonEntity
import com.example.data.local.TagEntity
import com.example.domain.model.BookStats
import com.example.domain.model.EntryVersion
import com.example.domain.model.JournalBook
import com.example.domain.model.JournalEntryDetail
import com.example.domain.model.JournalPrompt
import com.example.domain.model.MediaAttachment
import com.example.domain.model.MediaType
import com.example.domain.model.Person
import com.example.domain.model.PersonSummary
import com.example.domain.model.RichDocument
import com.example.domain.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.UUID

/**
 * Cloud-swap boundary: UI and ViewModels depend strictly on this interface
 * so local Room storage can later be synced or swapped with any cloud backend.
 */
interface JournalRepository {
    fun observeBooks(): Flow<List<JournalBook>>
    fun observeActiveEntries(): Flow<List<JournalEntryDetail>>
    fun observeTrashedEntries(): Flow<List<JournalEntryDetail>>
    fun observeEntry(entryId: String): Flow<JournalEntryDetail?>
    fun observeVersions(entryId: String): Flow<List<EntryVersion>>
    fun observeTags(): Flow<List<Tag>>
    fun observePeople(): Flow<List<Person>>
    fun observePrompts(): Flow<List<JournalPrompt>>

    suspend fun createBook(title: String, colorHex: String, iconName: String = "book"): JournalBook
    suspend fun deleteBook(bookId: String)
    suspend fun saveEntry(
        entryId: String?,
        bookId: String,
        title: String,
        document: RichDocument,
        entryTimestamp: Long,
        latitude: Double?,
        longitude: Double?,
        locationName: String?,
        cityName: String?,
        areaName: String?,
        streetName: String?,
        isFavorite: Boolean,
        media: List<MediaAttachment>,
        tagNames: List<String>,
        personIds: List<String>,
        recordVersion: Boolean = true,
        versionSummary: String = "Saved edit"
    ): String

    suspend fun setFavorite(entryId: String, favorite: Boolean)
    suspend fun moveEntry(entryId: String, targetBookId: String)
    suspend fun copyEntryToBook(entryId: String, targetBookId: String): String?
    suspend fun moveToTrash(entryId: String)
    suspend fun restoreFromTrash(entryId: String)
    suspend fun deletePermanently(entryId: String)
    suspend fun emptyTrash()

    suspend fun upsertPerson(displayName: String, contactLookupUri: String?, note: String = ""): Person
    suspend fun deletePerson(personId: String)
    suspend fun ensureTag(name: String): Tag

    suspend fun addCustomPrompt(category: String, promptText: String)
    suspend fun togglePromptFavorite(promptId: String, favorite: Boolean)
    suspend fun deleteCustomPrompt(promptId: String)
}

class LocalJournalRepository(private val dao: JournalDao) : JournalRepository {

    override fun observeBooks(): Flow<List<JournalBook>> =
        dao.observeBooks().map { list -> list.map { it.toDomain() } }

    override fun observeActiveEntries(): Flow<List<JournalEntryDetail>> =
        dao.observeActiveEntries().map { list -> list.map { it.toDomain() } }

    override fun observeTrashedEntries(): Flow<List<JournalEntryDetail>> =
        dao.observeTrashedEntries().map { list -> list.map { it.toDomain() } }

    override fun observeEntry(entryId: String): Flow<JournalEntryDetail?> =
        dao.observeEntryById(entryId).map { it?.toDomain() }

    override fun observeVersions(entryId: String): Flow<List<EntryVersion>> =
        dao.observeVersionsForEntry(entryId).map { list -> list.map { it.toDomain() } }

    override fun observeTags(): Flow<List<Tag>> =
        dao.observeAllTags().map { list -> list.map { Tag(it.id, it.name) } }

    override fun observePeople(): Flow<List<Person>> =
        dao.observeAllPeople().map { list -> list.map { it.toDomain() } }

    override fun observePrompts(): Flow<List<JournalPrompt>> =
        dao.observeAllPrompts().map { list -> list.map { it.toDomain() } }

    override suspend fun createBook(title: String, colorHex: String, iconName: String): JournalBook {
        val entity = JournalBookEntity(
            id = "book_${UUID.randomUUID()}",
            title = title.trim().ifEmpty { "Untitled Book" },
            colorHex = colorHex,
            iconName = iconName,
            createdAt = System.currentTimeMillis(),
            isDefault = false
        )
        dao.insertBook(entity)
        return entity.toDomain()
    }

    override suspend fun deleteBook(bookId: String) {
        dao.deleteBook(bookId)
    }

    override suspend fun saveEntry(
        entryId: String?,
        bookId: String,
        title: String,
        document: RichDocument,
        entryTimestamp: Long,
        latitude: Double?,
        longitude: Double?,
        locationName: String?,
        cityName: String?,
        areaName: String?,
        streetName: String?,
        isFavorite: Boolean,
        media: List<MediaAttachment>,
        tagNames: List<String>,
        personIds: List<String>,
        recordVersion: Boolean,
        versionSummary: String
    ): String {
        val now = System.currentTimeMillis()
        val id = entryId ?: "entry_${UUID.randomUUID()}"
        val encodedJson = RichDocument.encode(document)
        val plain = document.toPlainText()
        val derivedTitle = title.trim().ifEmpty {
            plain.lineSequence().firstOrNull { it.isNotBlank() }?.take(64) ?: "Untitled Entry"
        }

        val tagIds = tagNames
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .map { ensureTag(it).id }

        val entryEntity = JournalEntryEntity(
            id = id,
            bookId = bookId,
            title = derivedTitle,
            richContentJson = encodedJson,
            plainTextCache = plain,
            entryTimestamp = entryTimestamp,
            updatedAt = now,
            latitude = latitude,
            longitude = longitude,
            locationName = locationName,
            cityName = cityName,
            areaName = areaName,
            streetName = streetName,
            isFavorite = isFavorite,
            isTrashed = false,
            trashedAt = null
        )

        val mediaEntities = media.map {
            MediaAttachmentEntity(
                id = it.id,
                entryId = id,
                type = it.type.name,
                localUri = it.localUri,
                displayName = it.displayName,
                durationMs = it.durationMs,
                createdAt = it.createdAt
            )
        }

        val versionEntity = if (recordVersion) {
            EntryVersionEntity(
                id = "ver_${UUID.randomUUID()}",
                entryId = id,
                titleSnapshot = derivedTitle,
                richContentJson = encodedJson,
                savedAt = now,
                summaryLabel = versionSummary
            )
        } else null

        dao.saveFullEntry(
            entry = entryEntity,
            media = mediaEntities,
            tagIds = tagIds,
            personIds = personIds.distinct(),
            version = versionEntity
        )
        return id
    }

    override suspend fun setFavorite(entryId: String, favorite: Boolean) {
        dao.setEntryFavorite(entryId, favorite)
    }

    override suspend fun moveEntry(entryId: String, targetBookId: String) {
        dao.moveEntryToBook(entryId, targetBookId, System.currentTimeMillis())
    }

    override suspend fun copyEntryToBook(entryId: String, targetBookId: String): String? {
        val original = dao.getEntryByIdOnce(entryId) ?: return null
        val newId = "entry_${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        val copiedEntry = original.entry.copy(
            id = newId,
            bookId = targetBookId,
            title = "${original.entry.title} (Copy)",
            updatedAt = now
        )
        val copiedMedia = original.media.map {
            it.copy(id = "med_${UUID.randomUUID()}", entryId = newId)
        }
        val version = EntryVersionEntity(
            id = "ver_${UUID.randomUUID()}",
            entryId = newId,
            titleSnapshot = copiedEntry.title,
            richContentJson = copiedEntry.richContentJson,
            savedAt = now,
            summaryLabel = "Copied entry"
        )
        dao.saveFullEntry(
            entry = copiedEntry,
            media = copiedMedia,
            tagIds = original.tags.map { it.id },
            personIds = original.people.map { it.id },
            version = version
        )
        return newId
    }

    override suspend fun moveToTrash(entryId: String) {
        dao.moveToTrash(entryId, System.currentTimeMillis())
    }

    override suspend fun restoreFromTrash(entryId: String) {
        dao.restoreFromTrash(entryId)
    }

    override suspend fun deletePermanently(entryId: String) {
        dao.deleteEntryPermanently(entryId)
    }

    override suspend fun emptyTrash() {
        dao.emptyTrash()
    }

    override suspend fun upsertPerson(
        displayName: String,
        contactLookupUri: String?,
        note: String
    ): Person {
        val existing = dao.getAllPeopleOnce().firstOrNull {
            (contactLookupUri != null && it.contactLookupUri == contactLookupUri) ||
                it.displayName.equals(displayName.trim(), ignoreCase = true)
        }
        if (existing != null) return existing.toDomain()
        val entity = PersonEntity(
            id = "person_${UUID.randomUUID()}",
            displayName = displayName.trim().ifEmpty { "Unnamed Contact" },
            contactLookupUri = contactLookupUri,
            note = note.trim()
        )
        dao.insertPerson(entity)
        return entity.toDomain()
    }

    override suspend fun deletePerson(personId: String) {
        dao.deletePerson(personId)
    }

    override suspend fun ensureTag(name: String): Tag {
        val clean = name.trim()
        val existing = dao.findTagByName(clean)
        if (existing != null) return Tag(existing.id, existing.name)
        val created = TagEntity(id = "tag_${UUID.randomUUID()}", name = clean)
        dao.insertTag(created)
        return Tag(created.id, created.name)
    }

    override suspend fun addCustomPrompt(category: String, promptText: String) {
        if (promptText.isBlank()) return
        dao.insertPrompt(
            JournalPromptEntity(
                id = "prm_custom_${UUID.randomUUID()}",
                category = category,
                promptText = promptText.trim(),
                isCustom = true,
                isFavorite = false
            )
        )
    }

    override suspend fun togglePromptFavorite(promptId: String, favorite: Boolean) {
        dao.setPromptFavorite(promptId, favorite)
    }

    override suspend fun deleteCustomPrompt(promptId: String) {
        dao.deleteCustomPrompt(promptId)
    }
}

fun JournalBookEntity.toDomain() = JournalBook(
    id = id,
    title = title,
    colorHex = colorHex,
    iconName = iconName,
    createdAt = createdAt,
    isDefault = isDefault
)

fun PersonEntity.toDomain() = Person(
    id = id,
    displayName = displayName,
    contactLookupUri = contactLookupUri,
    note = note
)

fun EntryVersionEntity.toDomain() = EntryVersion(
    id = id,
    entryId = entryId,
    titleSnapshot = titleSnapshot,
    richContentJson = richContentJson,
    savedAt = savedAt,
    summaryLabel = summaryLabel
)

fun JournalPromptEntity.toDomain() = JournalPrompt(
    id = id,
    category = category,
    promptText = promptText,
    isCustom = isCustom,
    isFavorite = isFavorite
)

fun JournalEntryWithRelations.toDomain() = JournalEntryDetail(
    id = entry.id,
    bookId = entry.bookId,
    bookTitle = book?.title ?: "Diary",
    bookColorHex = book?.colorHex ?: "#F5B700",
    title = entry.title,
    richContentJson = entry.richContentJson,
    plainTextCache = entry.plainTextCache,
    entryTimestamp = entry.entryTimestamp,
    updatedAt = entry.updatedAt,
    latitude = entry.latitude,
    longitude = entry.longitude,
    locationName = entry.locationName,
    cityName = entry.cityName,
    areaName = entry.areaName,
    streetName = entry.streetName,
    isFavorite = entry.isFavorite,
    isTrashed = entry.isTrashed,
    trashedAt = entry.trashedAt,
    media = media.map {
        MediaAttachment(
            id = it.id,
            entryId = it.entryId,
            type = it.mediaType(),
            localUri = it.localUri,
            displayName = it.displayName,
            durationMs = it.durationMs,
            createdAt = it.createdAt
        )
    },
    tags = tags.map { Tag(it.id, it.name) },
    people = people.map { it.toDomain() }
)

fun computeBookStats(
    bookTitle: String,
    bookColorHex: String,
    entries: List<JournalEntryDetail>,
    referenceTimeMillis: Long = System.currentTimeMillis()
): BookStats {
    val cal = Calendar.getInstance()
    cal.timeInMillis = referenceTimeMillis
    val todayYear = cal.get(Calendar.YEAR)
    val todayMonth = cal.get(Calendar.MONTH)
    val todayDay = cal.get(Calendar.DAY_OF_MONTH)

    val weekStartCal = Calendar.getInstance().apply {
        timeInMillis = referenceTimeMillis
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val dayKeys = mutableSetOf<Long>() // epochDay-like key YYYYMMDD
    val years = mutableSetOf<Int>()
    var onThisDay = 0
    var thisWeek = 0
    val places = mutableSetOf<String>()
    val distinctTags = mutableSetOf<String>()
    var videos = 0
    var photos = 0
    var audios = 0
    var files = 0

    for (entry in entries) {
        cal.timeInMillis = entry.entryTimestamp
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH)
        val d = cal.get(Calendar.DAY_OF_MONTH)
        years.add(y)
        dayKeys.add((y * 10000L) + (m * 100L) + d)

        if (m == todayMonth && d == todayDay && y < todayYear) {
            onThisDay++
        }
        if (entry.entryTimestamp >= weekStartCal.timeInMillis) {
            thisWeek++
        }
        if (entry.latitude != null && entry.longitude != null) {
            val placeKey = entry.locationName?.takeIf { it.isNotBlank() }
                ?: "${"%.4f".format(entry.latitude)},${"%.4f".format(entry.longitude)}"
            places.add(placeKey)
        }
        entry.tags.forEach { distinctTags.add(it.id) }
        entry.media.forEach { mItem ->
            when (mItem.type) {
                MediaType.PHOTO -> photos++
                MediaType.VIDEO -> videos++
                MediaType.AUDIO -> audios++
                MediaType.DOCUMENT -> files++
            }
        }
    }

    // Current consecutive daily streak ending today or yesterday
    var streak = 0
    val streakCal = Calendar.getInstance().apply { timeInMillis = referenceTimeMillis }
    fun keyFor(c: Calendar): Long =
        (c.get(Calendar.YEAR) * 10000L) + (c.get(Calendar.MONTH) * 100L) + c.get(Calendar.DAY_OF_MONTH)

    if (!dayKeys.contains(keyFor(streakCal))) {
        streakCal.add(Calendar.DAY_OF_YEAR, -1)
    }
    while (dayKeys.contains(keyFor(streakCal))) {
        streak++
        streakCal.add(Calendar.DAY_OF_YEAR, -1)
    }

    val yearSpan = if (years.isEmpty()) {
        todayYear.toString()
    } else {
        val minY = years.min()
        val maxY = years.max()
        if (minY == maxY) "$minY" else "$minY - $maxY"
    }

    return BookStats(
        bookTitle = bookTitle,
        bookColorHex = bookColorHex,
        yearSpanLabel = yearSpan,
        streak = streak,
        entriesCount = entries.size,
        daysCount = dayKeys.size,
        mediaCount = photos + videos + audios + files,
        onThisDayCount = onThisDay,
        thisWeekCount = thisWeek,
        placesCount = places.size,
        tagsCount = distinctTags.size,
        videosCount = videos,
        photosCount = photos,
        audiosCount = audios,
        filesCount = files
    )
}

fun computePeopleSummaries(
    people: List<Person>,
    entries: List<JournalEntryDetail>
): List<PersonSummary> {
    return people.map { person ->
        val matching = entries.filter { e -> e.people.any { it.id == person.id } }
        PersonSummary(
            person = person,
            entriesCount = matching.size,
            lastInteractedAt = matching.maxOfOrNull { it.entryTimestamp }
        )
    }.sortedWith(
        compareByDescending<PersonSummary> { it.lastInteractedAt ?: 0L }
            .thenBy { it.person.displayName.lowercase() }
    )
}
