package com.example.ui.screens

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.FeedTab
import com.example.domain.model.JournalEntryDetail
import com.example.domain.model.MainSection
import com.example.domain.model.MediaFilter
import com.example.ui.components.AiComingSoonDialog
import com.example.ui.components.BookStatsOverviewPane
import com.example.ui.components.BooksDrawerContent
import com.example.ui.components.CreateBookDialog
import com.example.ui.components.PhoneBottomNavigationBar
import com.example.ui.components.StatTileAction
import com.example.ui.components.TabletNavigationRail
import com.example.ui.state.HeyBkUiState
import com.example.ui.state.HeyBkViewModel
import com.example.ui.theme.HeyBkTheme
import com.example.ui.theme.LocalHeyBkPalette
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeyBkApp(
    viewModel: HeyBkViewModel,
    windowSizeClass: WindowSizeClass
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.statusMessage) {
        val msg = state.statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeMessage()
        }
    }

    val isTabletLayout = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    var showCreateBookDialog by remember { mutableStateOf(false) }
    var showPhoneStatsSheet by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }
    var mapFocusEntry by remember { mutableStateOf<JournalEntryDetail?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    val onShareMarkdownExport: (JournalEntryDetail) -> Unit = { entry ->
        val md = viewModel.backupService.formatSingleEntryAsMarkdown(entry)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, entry.title)
            putExtra(Intent.EXTRA_TEXT, md)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export Entry"))
    }

    val onPinShortcut: (String, String) -> Unit = { id, title ->
        val ok = viewModel.mediaHelper.pinEntryToHomeScreen(id, title)
        viewModel.postMessage(
            if (ok) "Home screen shortcut requested" else "Pinned shortcuts not supported by launcher"
        )
    }

    val onStatTileClick: (StatTileAction) -> Unit = { action ->
        when (action) {
            StatTileAction.NONE -> {}
            StatTileAction.OPEN_LIST -> {
                showPhoneStatsSheet = false
                viewModel.selectSection(MainSection.BOOK_FEED)
                viewModel.selectFeedTab(FeedTab.LIST)
            }
            StatTileAction.OPEN_CALENDAR -> {
                showPhoneStatsSheet = false
                viewModel.selectSection(MainSection.BOOK_FEED)
                viewModel.selectFeedTab(FeedTab.CALENDAR)
            }
            StatTileAction.OPEN_MEDIA_ALL -> {
                showPhoneStatsSheet = false
                viewModel.selectSection(MainSection.BOOK_FEED)
                viewModel.setMediaFilter(MediaFilter.ALL)
                viewModel.selectFeedTab(FeedTab.MEDIA)
            }
            StatTileAction.OPEN_MEDIA_PHOTO -> {
                showPhoneStatsSheet = false
                viewModel.selectSection(MainSection.BOOK_FEED)
                viewModel.setMediaFilter(MediaFilter.PHOTOS)
                viewModel.selectFeedTab(FeedTab.MEDIA)
            }
            StatTileAction.OPEN_MEDIA_VIDEO -> {
                showPhoneStatsSheet = false
                viewModel.selectSection(MainSection.BOOK_FEED)
                viewModel.setMediaFilter(MediaFilter.VIDEO)
                viewModel.selectFeedTab(FeedTab.MEDIA)
            }
            StatTileAction.OPEN_MEDIA_AUDIO -> {
                showPhoneStatsSheet = false
                viewModel.selectSection(MainSection.BOOK_FEED)
                viewModel.setMediaFilter(MediaFilter.AUDIO)
                viewModel.selectFeedTab(FeedTab.MEDIA)
            }
            StatTileAction.OPEN_MEDIA_FILE -> {
                showPhoneStatsSheet = false
                viewModel.selectSection(MainSection.BOOK_FEED)
                viewModel.setMediaFilter(MediaFilter.FILES)
                viewModel.selectFeedTab(FeedTab.MEDIA)
            }
            StatTileAction.OPEN_MAP -> {
                showPhoneStatsSheet = false
                viewModel.selectSection(MainSection.BOOK_FEED)
                viewModel.selectFeedTab(FeedTab.MAP)
            }
            StatTileAction.OPEN_TAGS -> {
                showPhoneStatsSheet = false
                viewModel.selectSection(MainSection.TAGS)
            }
            StatTileAction.OPEN_TODAY -> {
                showPhoneStatsSheet = false
                viewModel.selectSection(MainSection.TODAY)
            }
        }
    }

    HeyBkTheme(darkTheme = state.settings.darkTheme) {
        val palette = LocalHeyBkPalette.current

        Scaffold(
            containerColor = palette.detailPaneBackground,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { _ ->
            if (isTabletLayout) {
                // 3-PANE TABLET SHELL (Matches attached Day One screenshots)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.railBackground)
                ) {
                    if (!state.isFullScreen) {
                        TabletNavigationRail(
                            section = state.section,
                            activeBookId = state.activeBookId,
                            books = state.books,
                            isExpanded = state.isRailExpanded,
                            onToggleExpand = viewModel::toggleRailExpanded,
                            onSelectSection = viewModel::selectSection,
                            onSelectBook = viewModel::selectBook,
                            onOpenNewBookDialog = { showCreateBookDialog = true }
                        )
                    }

                    val showSecondaryMasterPane = !state.isFullScreen && state.section == MainSection.BOOK_FEED
                    if (showSecondaryMasterPane) {
                        Box(
                            modifier = Modifier
                                .weight(0.42f)
                                .fillMaxHeight()
                                .padding(top = 8.dp, bottom = 8.dp, end = 8.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(palette.masterPaneBackground)
                        ) {
                            JournalMasterFeedPane(
                                state = state,
                                isCompactPhone = false,
                                mapFocusEntry = mapFocusEntry,
                                onOpenBooksMenu = viewModel::toggleRailExpanded,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onSelectTab = viewModel::selectFeedTab,
                                onSelectEntry = viewModel::openEntry,
                                onCreateNewEntry = { viewModel.startNewEntry() },
                                onDayClicked = { dayKey, dayMillis, matching ->
                                    if (matching.isNotEmpty()) {
                                        viewModel.setDateFilter(dayKey)
                                        viewModel.openEntry(matching.first().id)
                                    } else {
                                        viewModel.startNewEntry(presetTimestamp = dayMillis)
                                    }
                                },
                                onMediaFilterSelect = viewModel::setMediaFilter,
                                onJumpToMapForEntry = { entry ->
                                    mapFocusEntry = entry
                                    viewModel.selectFeedTab(FeedTab.MAP)
                                },
                                onRequestLocationPermission = {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_COARSE_LOCATION,
                                            Manifest.permission.ACCESS_FINE_LOCATION
                                        )
                                    )
                                },
                                onToggleFavoritesFilter = viewModel::toggleFavoritesFilter,
                                onClearFilters = viewModel::clearAllFilters,
                                onOpenStats = { viewModel.closeDetail() },
                                onOpenSettings = { viewModel.selectSection(MainSection.SETTINGS) }
                            )
                        }
                    }

                    // Right Pane (Detail / Editor / Stats / Active Section)
                    Box(
                        modifier = Modifier
                            .weight(if (showSecondaryMasterPane) 0.58f else 1f)
                            .fillMaxHeight()
                            .padding(
                                top = if (state.isFullScreen) 0.dp else 8.dp,
                                bottom = if (state.isFullScreen) 0.dp else 8.dp,
                                end = if (state.isFullScreen) 0.dp else 8.dp
                            )
                            .clip(if (state.isFullScreen) RoundedCornerShape(0.dp) else RoundedCornerShape(18.dp))
                            .background(palette.detailPaneBackground)
                    ) {
                        when {
                            state.isEditing || state.selectedEntry != null -> {
                                EntryDetailAndEditorPane(
                                    selectedEntry = state.selectedEntry,
                                    editorDraft = state.editorDraft,
                                    isEditing = state.isEditing,
                                    isFullScreen = state.isFullScreen,
                                    isCompactPhone = false,
                                    books = state.books,
                                    allTags = state.tags,
                                    allPeople = state.peopleSummaries,
                                    versions = state.selectedEntryVersions,
                                    mediaHelper = viewModel.mediaHelper,
                                    onBack = viewModel::closeDetail,
                                    onStartEditing = viewModel::startEditingExisting,
                                    onToggleFullScreen = viewModel::toggleFullScreen,
                                    onSaveDraft = { viewModel.saveCurrentDraft(exitEditor = true) },
                                    onUndo = viewModel::undoDraft,
                                    onRedo = viewModel::redoDraft,
                                    onToggleInEntrySearch = viewModel::toggleInEntrySearch,
                                    onUpdateInEntrySearch = viewModel::updateInEntrySearchQuery,
                                    onUpdateTitle = viewModel::updateDraftTitle,
                                    onFocusBlock = viewModel::setFocusedBlock,
                                    onUpdateBlockText = viewModel::updateDraftBlockText,
                                    onBackspaceEmptyBlock = viewModel::handleEmptyBackspace,
                                    onSetBlockType = viewModel::setDraftBlockType,
                                    onToggleChecklist = viewModel::toggleDraftChecklist,
                                    onToggleInlineMark = viewModel::toggleDraftInlineMark,
                                    onApplyTemplate = viewModel::applyTemplateToDraft,
                                    onAppendDictation = viewModel::appendDictationToDraft,
                                    onToggleFavorite = { id, fav ->
                                        if (id != null) viewModel.toggleEntryFavorite(id, fav) else viewModel.toggleDraftFavorite()
                                    },
                                    onMoveToBook = { id, targetBookId ->
                                        if (id != null && !state.isEditing) viewModel.moveEntryToBook(id, targetBookId)
                                        else viewModel.setDraftBook(targetBookId)
                                    },
                                    onCopyToBook = viewModel::copyEntryToBook,
                                    onTrashEntry = viewModel::trashEntry,
                                    onRestoreVersion = viewModel::restoreVersionIntoDraft,
                                    onExportEntry = onShareMarkdownExport,
                                    onPinToHomeScreen = onPinShortcut,
                                    onAddTag = viewModel::addDraftTag,
                                    onRemoveTag = viewModel::removeDraftTag,
                                    onAttachPerson = viewModel::attachPersonToDraft,
                                    onRemovePerson = viewModel::removePersonFromDraft,
                                    onTapPersonChip = viewModel::setPersonFilter,
                                    onTapTagChip = viewModel::setTagFilter,
                                    onAttachMediaUri = viewModel::attachUriToDraft,
                                    onAttachRecordedAudio = viewModel::attachRecordedAudioFile,
                                    onRemoveMedia = viewModel::removeDraftMedia,
                                    onRefreshGpsLocation = viewModel::refreshDraftLocation,
                                    onPickCustomLocation = viewModel::setDraftCustomLocation,
                                    onPostMessage = viewModel::postMessage
                                )
                            }
                            state.section == MainSection.BOOK_FEED -> {
                                BookStatsOverviewPane(
                                    stats = state.bookStats,
                                    onTileClick = onStatTileClick
                                )
                            }
                            else -> {
                                SecondaryWorkspaceContent(
                                    state = state,
                                    viewModel = viewModel,
                                    onShowAiInfo = { showAiDialog = true }
                                )
                            }
                        }
                    }
                }
            } else {
                // COMPACT PHONE SHELL
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val showFullDetail = state.isEditing || state.selectedEntry != null

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = !showFullDetail,
                    drawerContent = {
                        BooksDrawerContent(
                            activeBookId = state.activeBookId,
                            books = state.books,
                            onSelectBook = viewModel::selectBook,
                            onSelectSection = viewModel::selectSection,
                            onCreateBook = { showCreateBookDialog = true },
                            onCloseDrawer = { scope.launch { drawerState.close() } }
                        )
                    }
                ) {
                    if (showFullDetail) {
                        EntryDetailAndEditorPane(
                            selectedEntry = state.selectedEntry,
                            editorDraft = state.editorDraft,
                            isEditing = state.isEditing,
                            isFullScreen = state.isFullScreen,
                            isCompactPhone = true,
                            books = state.books,
                            allTags = state.tags,
                            allPeople = state.peopleSummaries,
                            versions = state.selectedEntryVersions,
                            mediaHelper = viewModel.mediaHelper,
                            onBack = viewModel::closeDetail,
                            onStartEditing = viewModel::startEditingExisting,
                            onToggleFullScreen = viewModel::toggleFullScreen,
                            onSaveDraft = { viewModel.saveCurrentDraft(exitEditor = true) },
                            onUndo = viewModel::undoDraft,
                            onRedo = viewModel::redoDraft,
                            onToggleInEntrySearch = viewModel::toggleInEntrySearch,
                            onUpdateInEntrySearch = viewModel::updateInEntrySearchQuery,
                            onUpdateTitle = viewModel::updateDraftTitle,
                            onFocusBlock = viewModel::setFocusedBlock,
                            onUpdateBlockText = viewModel::updateDraftBlockText,
                            onBackspaceEmptyBlock = viewModel::handleEmptyBackspace,
                            onSetBlockType = viewModel::setDraftBlockType,
                            onToggleChecklist = viewModel::toggleDraftChecklist,
                            onToggleInlineMark = viewModel::toggleDraftInlineMark,
                            onApplyTemplate = viewModel::applyTemplateToDraft,
                            onAppendDictation = viewModel::appendDictationToDraft,
                            onToggleFavorite = { id, fav ->
                                if (id != null) viewModel.toggleEntryFavorite(id, fav) else viewModel.toggleDraftFavorite()
                            },
                            onMoveToBook = { id, targetBookId ->
                                if (id != null && !state.isEditing) viewModel.moveEntryToBook(id, targetBookId)
                                else viewModel.setDraftBook(targetBookId)
                            },
                            onCopyToBook = viewModel::copyEntryToBook,
                            onTrashEntry = viewModel::trashEntry,
                            onRestoreVersion = viewModel::restoreVersionIntoDraft,
                            onExportEntry = onShareMarkdownExport,
                            onPinToHomeScreen = onPinShortcut,
                            onAddTag = viewModel::addDraftTag,
                            onRemoveTag = viewModel::removeDraftTag,
                            onAttachPerson = viewModel::attachPersonToDraft,
                            onRemovePerson = viewModel::removePersonFromDraft,
                            onTapPersonChip = { personId ->
                                viewModel.closeDetail()
                                viewModel.setPersonFilter(personId)
                            },
                            onTapTagChip = { tagId ->
                                viewModel.closeDetail()
                                viewModel.setTagFilter(tagId)
                            },
                            onAttachMediaUri = viewModel::attachUriToDraft,
                            onAttachRecordedAudio = viewModel::attachRecordedAudioFile,
                            onRemoveMedia = viewModel::removeDraftMedia,
                            onRefreshGpsLocation = viewModel::refreshDraftLocation,
                            onPickCustomLocation = viewModel::setDraftCustomLocation,
                            onPostMessage = viewModel::postMessage
                        )
                    } else {
                        Scaffold(
                            containerColor = palette.masterPaneBackground,
                            bottomBar = {
                                PhoneBottomNavigationBar(
                                    section = state.section,
                                    onSelectSection = viewModel::selectSection
                                )
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = innerPadding.calculateBottomPadding())
                            ) {
                                if (state.section == MainSection.BOOK_FEED) {
                                    JournalMasterFeedPane(
                                        state = state,
                                        isCompactPhone = true,
                                        mapFocusEntry = mapFocusEntry,
                                        onOpenBooksMenu = { scope.launch { drawerState.open() } },
                                        onSearchQueryChange = viewModel::updateSearchQuery,
                                        onSelectTab = viewModel::selectFeedTab,
                                        onSelectEntry = viewModel::openEntry,
                                        onCreateNewEntry = { viewModel.startNewEntry() },
                                        onDayClicked = { dayKey, dayMillis, matching ->
                                            if (matching.isNotEmpty()) {
                                                viewModel.setDateFilter(dayKey)
                                                viewModel.selectFeedTab(FeedTab.LIST)
                                            } else {
                                                viewModel.startNewEntry(presetTimestamp = dayMillis)
                                            }
                                        },
                                        onMediaFilterSelect = viewModel::setMediaFilter,
                                        onJumpToMapForEntry = { entry ->
                                            mapFocusEntry = entry
                                            viewModel.selectFeedTab(FeedTab.MAP)
                                        },
                                        onRequestLocationPermission = {
                                            locationPermissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                                    Manifest.permission.ACCESS_FINE_LOCATION
                                                )
                                            )
                                        },
                                        onToggleFavoritesFilter = viewModel::toggleFavoritesFilter,
                                        onClearFilters = viewModel::clearAllFilters,
                                        onOpenStats = { showPhoneStatsSheet = true },
                                        onOpenSettings = { viewModel.selectSection(MainSection.SETTINGS) }
                                    )
                                } else {
                                    SecondaryWorkspaceContent(
                                        state = state,
                                        viewModel = viewModel,
                                        onShowAiInfo = { showAiDialog = true }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showCreateBookDialog) {
            CreateBookDialog(
                onDismiss = { showCreateBookDialog = false },
                onConfirm = { title, hex ->
                    viewModel.createBook(title, hex)
                    showCreateBookDialog = false
                }
            )
        }

        if (showPhoneStatsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPhoneStatsSheet = false },
                containerColor = palette.detailPaneBackground
            ) {
                Box(modifier = Modifier.height(520.dp)) {
                    BookStatsOverviewPane(
                        stats = state.bookStats,
                        onTileClick = onStatTileClick
                    )
                }
            }
        }

        if (showAiDialog) {
            AiComingSoonDialog(onDismiss = { showAiDialog = false })
        }
    }
}

@Composable
private fun JournalMasterFeedPane(
    state: HeyBkUiState,
    isCompactPhone: Boolean,
    mapFocusEntry: JournalEntryDetail?,
    onOpenBooksMenu: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectTab: (FeedTab) -> Unit,
    onSelectEntry: (String) -> Unit,
    onCreateNewEntry: () -> Unit,
    onDayClicked: (dayKey: Long, dayMillis: Long, matchingEntries: List<JournalEntryDetail>) -> Unit,
    onMediaFilterSelect: (MediaFilter) -> Unit,
    onJumpToMapForEntry: (JournalEntryDetail) -> Unit,
    onRequestLocationPermission: () -> Unit,
    onToggleFavoritesFilter: () -> Unit,
    onClearFilters: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val palette = LocalHeyBkPalette.current
    var searchOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val activeBook = state.books.firstOrNull { it.id == state.activeBookId }
    val title = activeBook?.title ?: "All Entries"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(if (isCompactPhone) Modifier.statusBarsPadding() else Modifier)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar matching Screenshots 1 & 2
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCompactPhone) {
                    IconButton(
                        onClick = onOpenBooksMenu,
                        modifier = Modifier.testTag("phone_books_menu")
                    ) {
                        Icon(Icons.Outlined.Menu, contentDescription = "Open books", tint = palette.textPrimary)
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Reminder settings", tint = palette.textPrimary)
                }
                IconButton(
                    onClick = { searchOpen = !searchOpen },
                    modifier = Modifier.testTag("feed_search_toggle")
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = "Search entries", tint = palette.textPrimary)
                }
                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.testTag("feed_more_menu")
                    ) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Feed options", tint = palette.textPrimary)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Book Stats Overview") },
                            leadingIcon = { Icon(Icons.Outlined.BarChart, null) },
                            onClick = {
                                menuOpen = false
                                onOpenStats()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (state.favoritesOnly) "Show All Entries" else "Favorites Only") },
                            onClick = {
                                menuOpen = false
                                onToggleFavoritesFilter()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Clear Filters") },
                            onClick = {
                                menuOpen = false
                                onClearFilters()
                            }
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(palette.selectedCard)
                        .clickable(onClick = onOpenStats),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.settings.displayName.take(2).uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accentBlue
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = palette.textPrimary,
                modifier = Modifier.padding(horizontal = 18.dp)
            )

            if (searchOpen) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search entries, tags, people, places...") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            onSearchQueryChange("")
                            searchOpen = false
                        }) {
                            Icon(Icons.Rounded.Close, "Clear search")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("feed_search_input")
                )
            }

            // Active Filter Indicator Chip
            if (state.filterTagId != null || state.filterPersonId != null || state.filterDateDayKey != null || state.favoritesOnly) {
                val filterLabel = buildString {
                    if (state.favoritesOnly) append("Favorites ")
                    state.filterTagId?.let { id ->
                        val t = state.tags.firstOrNull { it.id == id }?.name
                        if (t != null) append("#$t ")
                    }
                    state.filterPersonId?.let { id ->
                        val p = state.peopleSummaries.firstOrNull { it.person.id == id }?.person?.displayName
                        if (p != null) append("@$p ")
                    }
                    if (state.filterDateDayKey != null) append("Selected Date")
                }.trim()
                Row(
                    modifier = Modifier
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.selectedCard)
                        .clickable(onClick = onClearFilters)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Filtered: $filterLabel", style = MaterialTheme.typography.labelSmall, color = palette.accentBlue)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Rounded.Close, "Clear filter", tint = palette.accentBlue, modifier = Modifier.size(14.dp))
                }
            }

            Spacer(Modifier.height(10.dp))

            // 4-Tab Bar: List | Calendar | Media | Map (matching Screenshots 1-6)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FeedTab.entries.forEach { tab ->
                    val selected = state.feedTab == tab
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectTab(tab) }
                            .padding(top = 6.dp)
                            .testTag("feed_tab_${tab.label.lowercase()}"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (selected) palette.accentBlue else palette.textSecondary,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .height(2.5.dp)
                                .width(38.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (selected) palette.accentBlue else Color.Transparent)
                        )
                    }
                }
            }
            HorizontalDivider(color = palette.hairline)

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (state.feedTab) {
                    FeedTab.LIST -> EntryListTab(
                        entries = state.filteredEntries,
                        selectedEntryId = state.selectedEntryId,
                        onSelectEntry = onSelectEntry,
                        onCreateNewEntry = onCreateNewEntry
                    )
                    FeedTab.CALENDAR -> CalendarTab(
                        entries = state.filteredEntries,
                        onDayClicked = onDayClicked
                    )
                    FeedTab.MEDIA -> MediaTab(
                        entries = state.filteredEntries,
                        selectedFilter = state.mediaFilter,
                        onSelectFilter = onMediaFilterSelect,
                        onOpenEntry = onSelectEntry,
                        onJumpToMapForEntry = onJumpToMapForEntry
                    )
                    FeedTab.MAP -> MapTab(
                        entries = state.filteredEntries,
                        focusEntry = mapFocusEntry,
                        onSelectEntry = onSelectEntry,
                        onRequestLocationPermission = onRequestLocationPermission
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onCreateNewEntry,
            containerColor = palette.fabContainer,
            contentColor = palette.fabContent,
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("New entry") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("new_entry_fab")
        )
    }
}

@Composable
private fun SecondaryWorkspaceContent(
    state: HeyBkUiState,
    viewModel: HeyBkViewModel,
    onShowAiInfo: () -> Unit
) {
    when (state.section) {
        MainSection.TODAY -> {
            val dailyPrompt = remember(state.prompts, state.selectedTodayMillis) {
                if (state.prompts.isEmpty()) "what's been the biggest part of your day so far?"
                else {
                    val dayIdx = ((state.selectedTodayMillis / 86_400_000L) % state.prompts.size).toInt().coerceAtLeast(0)
                    state.prompts[dayIdx].promptText
                }
            }
            TodayScreen(
                selectedMillis = state.selectedTodayMillis,
                displayName = state.settings.displayName,
                allEntries = state.allActiveEntries,
                dailyPrompt = dailyPrompt,
                onStepDate = viewModel::stepTodayDate,
                onResetToToday = { viewModel.setTodayDate(System.currentTimeMillis()) },
                onOpenEntry = { id ->
                    viewModel.selectSection(MainSection.BOOK_FEED)
                    viewModel.openEntry(id)
                },
                onStartNewEntryForDate = { millis, prompt ->
                    viewModel.startNewEntry(presetTimestamp = millis, presetPrompt = prompt)
                },
                onOpenSettings = { viewModel.selectSection(MainSection.SETTINGS) },
                onShowAiInfo = onShowAiInfo
            )
        }
        MainSection.PROMPTS -> PromptsScreen(
            prompts = state.prompts,
            onStartEntryWithPrompt = { prompt ->
                viewModel.startNewEntry(presetPrompt = prompt.promptText)
            },
            onToggleFavorite = viewModel::togglePromptFavorite,
            onAddCustomPrompt = viewModel::addCustomPrompt,
            onDeleteCustomPrompt = viewModel::deleteCustomPrompt
        )
        MainSection.PEOPLE -> PeopleDirectoryScreen(
            peopleSummaries = state.peopleSummaries,
            mediaHelper = viewModel.mediaHelper,
            onAddPerson = viewModel::addPerson,
            onDeletePerson = viewModel::deletePerson,
            onSelectPersonToFilter = viewModel::setPersonFilter,
            onPostMessage = viewModel::postMessage
        )
        MainSection.TAGS -> TagsDirectoryScreen(
            tags = state.tags,
            entries = state.allActiveEntries,
            onSelectTagToFilter = viewModel::setTagFilter
        )
        MainSection.TRASH -> TrashScreen(
            trashedEntries = state.trashedEntries,
            onRestore = viewModel::restoreTrashedEntry,
            onDeletePermanently = viewModel::deleteEntryPermanently,
            onEmptyTrash = viewModel::emptyTrash
        )
        MainSection.SETTINGS -> SettingsScreen(
            settings = state.settings,
            onToggleDarkTheme = viewModel::setDarkTheme,
            onUpdateReminder = viewModel::setReminder,
            onUpdateDisplayName = viewModel::setDisplayName,
            onExportBackup = viewModel::exportBackup,
            onImportBackup = viewModel::importBackup,
            onNavigateSection = viewModel::selectSection
        )
        MainSection.BOOK_FEED -> {}
    }
}
