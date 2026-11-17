package com.example.ui.state

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppPreferencesDataStore
import com.example.data.local.HeyBkDatabase
import com.example.data.local.HeyBkSettings
import com.example.data.repository.BackupArchiveService
import com.example.data.repository.LocalJournalRepository
import com.example.data.repository.computeBookStats
import com.example.data.repository.computePeopleSummaries
import com.example.domain.model.BlockType
import com.example.domain.model.BookStats
import com.example.domain.model.EntryTemplate
import com.example.domain.model.EntryVersion
import com.example.domain.model.FeedTab
import com.example.domain.model.InlineMarkType
import com.example.domain.model.JournalBook
import com.example.domain.model.JournalEntryDetail
import com.example.domain.model.JournalPrompt
import com.example.domain.model.MainSection
import com.example.domain.model.MediaAttachment
import com.example.domain.model.MediaFilter
import com.example.domain.model.MediaType
import com.example.domain.model.Person
import com.example.domain.model.PersonSummary
import com.example.domain.model.RichBlock
import com.example.domain.model.RichDocument
import com.example.domain.model.Tag
import com.example.system.CapturedLocation
import com.example.system.DailyReminderScheduler
import com.example.system.LocationHelper
import com.example.system.MediaStorageHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

data class HeyBkUiState(
    val section: MainSection = MainSection.BOOK_FEED,
    val feedTab: FeedTab = FeedTab.LIST,
    val activeBookId: String? = null, // null = All Entries
    val searchQuery: String = "",
    val filterTagId: String? = null,
    val filterPersonId: String? = null,
    val filterDateDayKey: Long? = null,
    val favoritesOnly: Boolean = false,
    val mediaFilter: MediaFilter = MediaFilter.ALL,
    val selectedEntryId: String? = null,
    val isEditing: Boolean = false,
    val isFullScreen: Boolean = false,
    val selectedTodayMillis: Long = System.currentTimeMillis(),
    val books: List<JournalBook> = emptyList(),
    val allActiveEntries: List<JournalEntryDetail> = emptyList(),
    val filteredEntries: List<JournalEntryDetail> = emptyList(),
    val trashedEntries: List<JournalEntryDetail> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val peopleSummaries: List<PersonSummary> = emptyList(),
    val prompts: List<JournalPrompt> = emptyList(),
    val bookStats: BookStats = computeBookStats("All Entries", "#78C6FF", emptyList()),
    val selectedEntry: JournalEntryDetail? = null,
    val selectedEntryVersions: List<EntryVersion> = emptyList(),
    val editorDraft: EditorDraftState? = null,
    val settings: HeyBkSettings = HeyBkSettings(),
    val isRailExpanded: Boolean = false,
    val statusMessage: String? = null
)

private data class FilterNavState(
    val section: MainSection = MainSection.BOOK_FEED,
    val feedTab: FeedTab = FeedTab.LIST,
    val activeBookId: String? = null,
    val searchQuery: String = "",
    val filterTagId: String? = null,
    val filterPersonId: String? = null,
    val filterDateDayKey: Long? = null,
    val favoritesOnly: Boolean = false,
    val mediaFilter: MediaFilter = MediaFilter.ALL,
    val selectedEntryId: String? = null,
    val isEditing: Boolean = false,
    val isFullScreen: Boolean = false,
    val selectedTodayMillis: Long = System.currentTimeMillis(),
    val isRailExpanded: Boolean = false,
    val statusMessage: String? = null
)

class HeyBkViewModel(application: Application) : AndroidViewModel(application) {

    private val db = HeyBkDatabase.getInstance(application)
    private val repository = LocalJournalRepository(db.journalDao())
    private val prefs = AppPreferencesDataStore(application)
    val backupService = BackupArchiveService(application, db)
    val locationHelper = LocationHelper(application)
    val mediaHelper = MediaStorageHelper(application)

    private val navState = MutableStateFlow(FilterNavState())
    private val editorDraftFlow = MutableStateFlow<EditorDraftState?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val versionsFlow = navState.flatMapLatest { nav ->
        val id = nav.selectedEntryId ?: editorDraftFlow.value?.entryId
        if (id != null) repository.observeVersions(id) else flowOf(emptyList())
    }

    init {
        viewModelScope.launch {
            prefs.settingsFlow.collect { s ->
                DailyReminderScheduler.schedule(
                    context = getApplication(),
                    enabled = s.reminderEnabled,
                    hour = s.reminderHour,
                    minute = s.reminderMinute
                )
            }
        }
    }

    private val coreDataFlow = combine(
        repository.observeBooks(),
        repository.observeActiveEntries(),
        repository.observeTrashedEntries(),
        repository.observeTags(),
        repository.observePeople(),
        repository.observePrompts(),
        prefs.settingsFlow
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        CoreDataBundle(
            books = arr[0] as List<JournalBook>,
            activeEntries = arr[1] as List<JournalEntryDetail>,
            trashedEntries = arr[2] as List<JournalEntryDetail>,
            tags = arr[3] as List<Tag>,
            people = arr[4] as List<Person>,
            prompts = arr[5] as List<JournalPrompt>,
            settings = arr[6] as HeyBkSettings
        )
    }

    val uiState: StateFlow<HeyBkUiState> = combine(
        navState,
        coreDataFlow,
        editorDraftFlow,
        versionsFlow
    ) { nav, core, draft, versions ->
        val activeBook = core.books.firstOrNull { it.id == nav.activeBookId }
        val bookEntries = if (nav.activeBookId == null) {
            core.activeEntries
        } else {
            core.activeEntries.filter { it.bookId == nav.activeBookId }
        }

        val filtered = bookEntries.filter { entry ->
            val matchesSearch = nav.searchQuery.isBlank() ||
                entry.title.contains(nav.searchQuery, ignoreCase = true) ||
                entry.plainTextCache.contains(nav.searchQuery, ignoreCase = true) ||
                entry.tags.any { it.name.contains(nav.searchQuery, ignoreCase = true) } ||
                entry.people.any { it.displayName.contains(nav.searchQuery, ignoreCase = true) } ||
                (entry.locationName?.contains(nav.searchQuery, ignoreCase = true) == true)

            val matchesTag = nav.filterTagId == null || entry.tags.any { it.id == nav.filterTagId }
            val matchesPerson = nav.filterPersonId == null || entry.people.any { it.id == nav.filterPersonId }
            val matchesFav = !nav.favoritesOnly || entry.isFavorite
            val matchesDate = nav.filterDateDayKey == null || dayKeyOf(entry.entryTimestamp) == nav.filterDateDayKey

            matchesSearch && matchesTag && matchesPerson && matchesFav && matchesDate
        }

        val statsTitle = activeBook?.title ?: "All Entries"
        val statsColor = activeBook?.colorHex ?: "#78C6FF"
        val stats = computeBookStats(statsTitle, statsColor, bookEntries)
        val selectedEntry = core.activeEntries.firstOrNull { it.id == nav.selectedEntryId }
            ?: core.trashedEntries.firstOrNull { it.id == nav.selectedEntryId }

        HeyBkUiState(
            section = nav.section,
            feedTab = nav.feedTab,
            activeBookId = nav.activeBookId,
            searchQuery = nav.searchQuery,
            filterTagId = nav.filterTagId,
            filterPersonId = nav.filterPersonId,
            filterDateDayKey = nav.filterDateDayKey,
            favoritesOnly = nav.favoritesOnly,
            mediaFilter = nav.mediaFilter,
            selectedEntryId = nav.selectedEntryId,
            isEditing = nav.isEditing,
            isFullScreen = nav.isFullScreen,
            selectedTodayMillis = nav.selectedTodayMillis,
            books = core.books,
            allActiveEntries = core.activeEntries,
            filteredEntries = filtered,
            trashedEntries = core.trashedEntries,
            tags = core.tags,
            peopleSummaries = computePeopleSummaries(core.people, core.activeEntries),
            prompts = core.prompts,
            bookStats = stats,
            selectedEntry = selectedEntry,
            selectedEntryVersions = versions,
            editorDraft = draft,
            settings = core.settings,
            isRailExpanded = nav.isRailExpanded,
            statusMessage = nav.statusMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HeyBkUiState()
    )

    // Navigation & Filtering
    fun selectSection(section: MainSection) {
        navState.update {
            it.copy(
                section = section,
                isEditing = false,
                isFullScreen = false,
                selectedEntryId = if (section != MainSection.BOOK_FEED) null else it.selectedEntryId
            )
        }
    }

    fun selectBook(bookId: String?) {
        navState.update {
            it.copy(
                section = MainSection.BOOK_FEED,
                activeBookId = bookId,
                selectedEntryId = null,
                isEditing = false,
                isFullScreen = false,
                filterDateDayKey = null
            )
        }
    }

    fun selectFeedTab(tab: FeedTab) {
        navState.update { it.copy(feedTab = tab) }
    }

    fun toggleRailExpanded() {
        navState.update { it.copy(isRailExpanded = !it.isRailExpanded) }
    }

    fun updateSearchQuery(query: String) {
        navState.update { it.copy(searchQuery = query) }
    }

    fun setTagFilter(tagId: String?) {
        navState.update {
            it.copy(
                section = MainSection.BOOK_FEED,
                feedTab = FeedTab.LIST,
                filterTagId = tagId
            )
        }
    }

    fun setPersonFilter(personId: String?) {
        navState.update {
            it.copy(
                section = MainSection.BOOK_FEED,
                feedTab = FeedTab.LIST,
                filterPersonId = personId
            )
        }
    }

    fun setDateFilter(dayKey: Long?) {
        navState.update {
            it.copy(
                section = MainSection.BOOK_FEED,
                feedTab = FeedTab.LIST,
                filterDateDayKey = dayKey
            )
        }
    }

    fun toggleFavoritesFilter() {
        navState.update { it.copy(favoritesOnly = !it.favoritesOnly) }
    }

    fun clearAllFilters() {
        navState.update {
            it.copy(
                searchQuery = "",
                filterTagId = null,
                filterPersonId = null,
                filterDateDayKey = null,
                favoritesOnly = false
            )
        }
    }

    fun setMediaFilter(filter: MediaFilter) {
        navState.update { it.copy(mediaFilter = filter) }
    }

    fun stepTodayDate(deltaDays: Int) {
        navState.update { state ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = state.selectedTodayMillis
                add(Calendar.DAY_OF_YEAR, deltaDays)
            }
            state.copy(selectedTodayMillis = cal.timeInMillis)
        }
    }

    fun setTodayDate(millis: Long) {
        navState.update { it.copy(selectedTodayMillis = millis) }
    }

    // Selection & Editor lifecycle
    fun openEntry(entryId: String) {
        editorDraftFlow.value = null
        navState.update {
            it.copy(
                selectedEntryId = entryId,
                isEditing = false,
                isFullScreen = false
            )
        }
    }

    fun closeDetail() {
        editorDraftFlow.value = null
        navState.update {
            it.copy(
                selectedEntryId = null,
                isEditing = false,
                isFullScreen = false
            )
        }
    }

    fun startEditingExisting(entry: JournalEntryDetail) {
        editorDraftFlow.value = EditorDraftState.fromEntry(entry)
        navState.update {
            it.copy(
                selectedEntryId = entry.id,
                isEditing = true
            )
        }
    }

    fun startNewEntry(
        presetBookId: String? = null,
        presetTimestamp: Long = System.currentTimeMillis(),
        presetPrompt: String? = null
    ) {
        val current = uiState.value
        val targetBookId = presetBookId
            ?: current.activeBookId
            ?: current.settings.defaultBookId.takeIf { id -> current.books.any { it.id == id } }
            ?: current.books.firstOrNull()?.id
            ?: "book_diary"

        val initialBlocks = if (!presetPrompt.isNullOrBlank()) {
            listOf(
                RichBlock(type = BlockType.QUOTE, text = presetPrompt),
                RichBlock(type = BlockType.NORMAL, text = "")
            )
        } else {
            listOf(RichBlock())
        }

        editorDraftFlow.value = EditorDraftState(
            entryId = null,
            bookId = targetBookId,
            title = "",
            blocks = initialBlocks,
            focusedBlockIndex = initialBlocks.lastIndex,
            entryTimestamp = presetTimestamp,
            isCapturingLocation = locationHelper.hasLocationPermission()
        )
        navState.update {
            it.copy(
                section = MainSection.BOOK_FEED,
                selectedEntryId = null,
                isEditing = true,
                isFullScreen = false
            )
        }

        if (locationHelper.hasLocationPermission()) {
            refreshDraftLocation()
        }
    }

    fun refreshDraftLocation() {
        viewModelScope.launch {
            editorDraftFlow.update { it?.copy(isCapturingLocation = true) }
            val loc = locationHelper.fetchCurrentLocation()
            editorDraftFlow.update { draft ->
                if (draft == null) null
                else if (loc != null) {
                    draft.copy(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        locationName = loc.locationName,
                        cityName = loc.cityName,
                        areaName = loc.areaName,
                        streetName = loc.streetName,
                        isCapturingLocation = false
                    )
                } else {
                    draft.copy(isCapturingLocation = false)
                }
            }
        }
    }

    fun setDraftCustomLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val resolved: CapturedLocation = locationHelper.reverseGeocode(latitude, longitude)
            editorDraftFlow.update {
                it?.copy(
                    latitude = resolved.latitude,
                    longitude = resolved.longitude,
                    locationName = resolved.locationName,
                    cityName = resolved.cityName,
                    areaName = resolved.areaName,
                    streetName = resolved.streetName
                )
            }
        }
    }

    fun toggleFullScreen() {
        navState.update { it.copy(isFullScreen = !it.isFullScreen) }
    }

    // Draft mutations
    fun updateDraftTitle(title: String) {
        editorDraftFlow.update { draft ->
            draft?.pushUndo()?.copy(title = title)
        }
    }

    fun setFocusedBlock(index: Int, selStart: Int = 0, selEnd: Int = 0) {
        editorDraftFlow.update { draft ->
            draft?.copy(
                focusedBlockIndex = index.coerceIn(0, (draft.blocks.size - 1).coerceAtLeast(0)),
                selectionStart = selStart,
                selectionEnd = selEnd
            )
        }
    }

    fun updateDraftBlockText(index: Int, newText: String) {
        editorDraftFlow.update { it?.withUpdatedBlockText(index, newText) }
    }

    fun setDraftBlockType(type: BlockType) {
        editorDraftFlow.update { draft ->
            draft?.withBlockType(draft.focusedBlockIndex, type)
        }
    }

    fun toggleDraftChecklist(index: Int) {
        editorDraftFlow.update { it?.withChecklistToggled(index) }
    }

    fun handleEmptyBackspace(index: Int) {
        editorDraftFlow.update { it?.withMergedOrDeletedEmptyBlock(index) }
    }

    fun toggleDraftInlineMark(mark: InlineMarkType, url: String? = null) {
        editorDraftFlow.update { draft ->
            draft?.withInlineMarkToggled(
                blockIndex = draft.focusedBlockIndex,
                start = draft.selectionStart,
                end = draft.selectionEnd,
                mark = mark,
                url = url
            )
        }
    }

    fun undoDraft() {
        editorDraftFlow.update { it?.undo() }
    }

    fun redoDraft() {
        editorDraftFlow.update { it?.redo() }
    }

    fun toggleInEntrySearch() {
        editorDraftFlow.update {
            it?.copy(inEntrySearchVisible = !it.inEntrySearchVisible, inEntrySearchQuery = "")
        }
    }

    fun updateInEntrySearchQuery(query: String) {
        editorDraftFlow.update { it?.copy(inEntrySearchQuery = query) }
    }

    fun applyTemplateToDraft(template: EntryTemplate) {
        editorDraftFlow.update { it?.withTemplateApplied(template) }
    }

    fun appendDictationToDraft(spoken: String) {
        editorDraftFlow.update { it?.withAppendedDictation(spoken) }
    }

    fun setDraftBook(bookId: String) {
        editorDraftFlow.update { it?.copy(bookId = bookId) }
    }

    fun toggleDraftFavorite() {
        editorDraftFlow.update { it?.copy(isFavorite = !it.isFavorite) }
    }

    fun addDraftTag(tagName: String) {
        val clean = tagName.trim()
        if (clean.isEmpty()) return
        editorDraftFlow.update { draft ->
            if (draft == null) null
            else if (draft.tagNames.any { it.equals(clean, ignoreCase = true) }) draft
            else draft.copy(tagNames = draft.tagNames + clean)
        }
    }

    fun removeDraftTag(tagName: String) {
        editorDraftFlow.update { draft ->
            draft?.copy(tagNames = draft.tagNames.filterNot { it == tagName })
        }
    }

    fun attachPersonToDraft(name: String, contactUri: String? = null) {
        viewModelScope.launch {
            val person = repository.upsertPerson(name, contactUri)
            editorDraftFlow.update { draft ->
                if (draft == null) null
                else if (draft.people.any { it.id == person.id }) draft
                else draft.copy(people = draft.people + person)
            }
        }
    }

    fun removePersonFromDraft(personId: String) {
        editorDraftFlow.update { draft ->
            draft?.copy(people = draft.people.filterNot { it.id == personId })
        }
    }

    fun attachUriToDraft(uri: Uri, type: MediaType) {
        viewModelScope.launch {
            val attachment = mediaHelper.copyIntoAppStorage(uri, type)
            if (attachment != null) {
                editorDraftFlow.update { draft ->
                    draft?.copy(media = draft.media + attachment)
                }
            } else {
                postMessage("Unable to attach selected file.")
            }
        }
    }

    fun attachRecordedAudioFile(fileUri: String, displayName: String) {
        val attachment = MediaAttachment(
            id = "med_${UUID.randomUUID()}",
            entryId = editorDraftFlow.value?.entryId ?: "",
            type = MediaType.AUDIO,
            localUri = fileUri,
            displayName = displayName,
            createdAt = System.currentTimeMillis()
        )
        editorDraftFlow.update { draft ->
            draft?.copy(media = draft.media + attachment)
        }
    }

    fun removeDraftMedia(mediaId: String) {
        editorDraftFlow.update { draft ->
            draft?.copy(media = draft.media.filterNot { it.id == mediaId })
        }
    }

    fun restoreVersionIntoDraft(version: EntryVersion) {
        val doc = RichDocument.decode(version.richContentJson)
        editorDraftFlow.update { draft ->
            draft?.pushUndo()?.copy(
                title = version.titleSnapshot,
                blocks = doc.blocks.ifEmpty { listOf(RichBlock()) }
            )
        }
        postMessage("Restored version from earlier edit. Tap ✓ to save.")
    }

    fun saveCurrentDraft(exitEditor: Boolean = true) {
        val draft = editorDraftFlow.value ?: return
        viewModelScope.launch {
            val savedId = repository.saveEntry(
                entryId = draft.entryId,
                bookId = draft.bookId,
                title = draft.title,
                document = draft.toDocument(),
                entryTimestamp = draft.entryTimestamp,
                latitude = draft.latitude,
                longitude = draft.longitude,
                locationName = draft.locationName,
                cityName = draft.cityName,
                areaName = draft.areaName,
                streetName = draft.streetName,
                isFavorite = draft.isFavorite,
                media = draft.media,
                tagNames = draft.tagNames,
                personIds = draft.people.map { it.id },
                recordVersion = true,
                versionSummary = if (draft.entryId == null) "Created entry" else "Edited (${draft.toDocument().wordCount()} words)"
            )
            if (exitEditor) {
                editorDraftFlow.value = null
                navState.update {
                    it.copy(
                        selectedEntryId = savedId,
                        isEditing = false,
                        isFullScreen = false,
                        statusMessage = "Entry saved"
                    )
                }
            } else {
                editorDraftFlow.update { it?.copy(entryId = savedId) }
                navState.update { it.copy(selectedEntryId = savedId, statusMessage = "Entry saved") }
            }
        }
    }

    // Entry actions (Reader & Editor 3-dots)
    fun toggleEntryFavorite(entryId: String, currentlyFavorite: Boolean) {
        viewModelScope.launch {
            repository.setFavorite(entryId, !currentlyFavorite)
            editorDraftFlow.update { if (it?.entryId == entryId) it.copy(isFavorite = !currentlyFavorite) else it }
        }
    }

    fun moveEntryToBook(entryId: String, targetBookId: String) {
        viewModelScope.launch {
            repository.moveEntry(entryId, targetBookId)
            editorDraftFlow.update { if (it?.entryId == entryId) it.copy(bookId = targetBookId) else it }
            postMessage("Moved entry")
        }
    }

    fun copyEntryToBook(entryId: String, targetBookId: String) {
        viewModelScope.launch {
            val copiedId = repository.copyEntryToBook(entryId, targetBookId)
            if (copiedId != null) postMessage("Copied entry to selected book")
        }
    }

    fun trashEntry(entryId: String) {
        viewModelScope.launch {
            repository.moveToTrash(entryId)
            editorDraftFlow.value = null
            navState.update {
                it.copy(
                    selectedEntryId = null,
                    isEditing = false,
                    isFullScreen = false,
                    statusMessage = "Moved to Trash"
                )
            }
        }
    }

    fun restoreTrashedEntry(entryId: String) {
        viewModelScope.launch {
            repository.restoreFromTrash(entryId)
            postMessage("Restored entry")
        }
    }

    fun deleteEntryPermanently(entryId: String) {
        viewModelScope.launch {
            repository.deletePermanently(entryId)
            postMessage("Deleted permanently")
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
            postMessage("Trash emptied")
        }
    }

    // Books
    fun createBook(title: String, colorHex: String) {
        viewModelScope.launch {
            val created = repository.createBook(title, colorHex)
            selectBook(created.id)
            postMessage("Created book \"${created.title}\"")
        }
    }

    fun deleteBook(bookId: String) {
        viewModelScope.launch {
            repository.deleteBook(bookId)
            if (navState.value.activeBookId == bookId) {
                selectBook(null)
            }
            postMessage("Deleted book")
        }
    }

    // People Directory
    fun addPerson(name: String, contactUri: String? = null, note: String = "") {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.upsertPerson(name, contactUri, note)
            postMessage("Added $name")
        }
    }

    fun deletePerson(personId: String) {
        viewModelScope.launch {
            repository.deletePerson(personId)
        }
    }

    // Prompts
    fun addCustomPrompt(category: String, text: String) {
        viewModelScope.launch {
            repository.addCustomPrompt(category, text)
            postMessage("Prompt saved")
        }
    }

    fun togglePromptFavorite(prompt: JournalPrompt) {
        viewModelScope.launch {
            repository.togglePromptFavorite(prompt.id, !prompt.isFavorite)
        }
    }

    fun deleteCustomPrompt(promptId: String) {
        viewModelScope.launch {
            repository.deleteCustomPrompt(promptId)
        }
    }

    // Settings & Backup
    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { prefs.setDarkTheme(enabled) }
    }

    fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            prefs.setReminder(enabled, hour, minute)
            DailyReminderScheduler.schedule(getApplication(), enabled, hour, minute)
            val timeStr = String.format("%02d:%02d", hour, minute)
            postMessage(if (enabled) "Daily reminder set for $timeStr" else "Daily reminder paused")
        }
    }

    fun setDisplayName(name: String) {
        viewModelScope.launch { prefs.setDisplayName(name) }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val result = backupService.exportToZipUri(uri)
            result.onSuccess { count ->
                postMessage("Exported $count entries to Hey Bk backup archive")
            }.onFailure { err ->
                postMessage("Export failed: ${err.localizedMessage}")
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            val result = backupService.importFromUri(uri)
            result.onSuccess { summary ->
                postMessage("Imported ${summary.entriesImported} entries, ${summary.booksImported} books, ${summary.mediaRestored} media files")
            }.onFailure { err ->
                postMessage("Import failed: ${err.localizedMessage}")
            }
        }
    }

    fun postMessage(msg: String) {
        navState.update { it.copy(statusMessage = msg) }
    }

    fun consumeMessage() {
        navState.update { it.copy(statusMessage = null) }
    }

    private data class CoreDataBundle(
        val books: List<JournalBook>,
        val activeEntries: List<JournalEntryDetail>,
        val trashedEntries: List<JournalEntryDetail>,
        val tags: List<Tag>,
        val people: List<Person>,
        val prompts: List<JournalPrompt>,
        val settings: HeyBkSettings
    )
}

fun dayKeyOf(timestampMillis: Long): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = timestampMillis }
    return (cal.get(Calendar.YEAR) * 10000L) +
        (cal.get(Calendar.MONTH) * 100L) +
        cal.get(Calendar.DAY_OF_MONTH)
}
