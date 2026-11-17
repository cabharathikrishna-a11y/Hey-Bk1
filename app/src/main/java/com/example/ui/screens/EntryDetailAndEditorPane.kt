package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.outlined.AddToHomeScreen
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloseFullscreen
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.domain.model.BlockType
import com.example.domain.model.EntryTemplate
import com.example.domain.model.EntryVersion
import com.example.domain.model.InlineMarkType
import com.example.domain.model.JournalBook
import com.example.domain.model.JournalEntryDetail
import com.example.domain.model.MediaAttachment
import com.example.domain.model.MediaType
import com.example.domain.model.PersonSummary
import com.example.domain.model.RichBlock
import com.example.domain.model.RichDocument
import com.example.domain.model.Tag
import com.example.domain.model.toAnnotatedString
import com.example.system.MediaStorageHelper
import com.example.ui.components.AiComingSoonDialog
import com.example.ui.components.BookPickerDialog
import com.example.ui.components.EntryInfoDialog
import com.example.ui.components.HyperlinkDialog
import com.example.ui.components.TagAndPeopleSheet
import com.example.ui.components.TemplatesBottomSheet
import com.example.ui.components.VersionHistoryBottomSheet
import com.example.ui.state.EditorDraftState
import com.example.ui.theme.LocalHeyBkPalette
import com.example.ui.theme.LoraFontFamily
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.parseColorHex
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryDetailAndEditorPane(
    selectedEntry: JournalEntryDetail?,
    editorDraft: EditorDraftState?,
    isEditing: Boolean,
    isFullScreen: Boolean,
    isCompactPhone: Boolean,
    books: List<JournalBook>,
    allTags: List<Tag>,
    allPeople: List<PersonSummary>,
    versions: List<EntryVersion>,
    mediaHelper: MediaStorageHelper,
    onBack: () -> Unit,
    onStartEditing: (JournalEntryDetail) -> Unit,
    onToggleFullScreen: () -> Unit,
    onSaveDraft: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleInEntrySearch: () -> Unit,
    onUpdateInEntrySearch: (String) -> Unit,
    onUpdateTitle: (String) -> Unit,
    onFocusBlock: (Int, Int, Int) -> Unit,
    onUpdateBlockText: (Int, String) -> Unit,
    onBackspaceEmptyBlock: (Int) -> Unit,
    onSetBlockType: (BlockType) -> Unit,
    onToggleChecklist: (Int) -> Unit,
    onToggleInlineMark: (InlineMarkType, String?) -> Unit,
    onApplyTemplate: (EntryTemplate) -> Unit,
    onAppendDictation: (String) -> Unit,
    onToggleFavorite: (String?, Boolean) -> Unit,
    onMoveToBook: (String?, String) -> Unit,
    onCopyToBook: (String, String) -> Unit,
    onTrashEntry: (String) -> Unit,
    onRestoreVersion: (EntryVersion) -> Unit,
    onExportEntry: (JournalEntryDetail) -> Unit,
    onPinToHomeScreen: (String, String) -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onAttachPerson: (String, String?) -> Unit,
    onRemovePerson: (String) -> Unit,
    onTapPersonChip: (String) -> Unit,
    onTapTagChip: (String) -> Unit,
    onAttachMediaUri: (Uri, MediaType) -> Unit,
    onAttachRecordedAudio: (String, String) -> Unit,
    onRemoveMedia: (String) -> Unit,
    onRefreshGpsLocation: () -> Unit,
    onPickCustomLocation: (Double, Double) -> Unit,
    onPostMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalHeyBkPalette.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val headerDateFmt = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()) }
    val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    if (isCompactPhone || isFullScreen || isEditing) {
        BackHandler {
            if (isEditing) onSaveDraft() else onBack()
        }
    }

    var showOverflowMenu by remember { mutableStateOf(false) }
    var showBottomOverflow by remember { mutableStateOf(false) }
    var showTagsPeopleSheet by remember { mutableStateOf(false) }
    var showTemplatesSheet by remember { mutableStateOf(false) }
    var showVersionHistory by remember { mutableStateOf(false) }
    var showMoveBookDialog by remember { mutableStateOf(false) }
    var showCopyBookDialog by remember { mutableStateOf(false) }
    var showEntryInfo by remember { mutableStateOf(false) }
    var showHyperlinkDialog by remember { mutableStateOf(false) }
    var showLocationPicker by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }
    var useSerifWritingFont by remember { mutableStateOf(false) }
    var zoomedPhotoUri by remember { mutableStateOf<String?>(null) }
    var playingVideoUri by remember { mutableStateOf<String?>(null) }
    var playingAudio by remember { mutableStateOf<MediaAttachment?>(null) }
    var readerSearchOpen by remember { mutableStateOf(false) }
    var readerSearchQuery by remember { mutableStateOf("") }

    // Camera / Media / Document / Contact / Dictation launchers
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingVideoUri by remember { mutableStateOf<Uri?>(null) }
    var activeRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var activeRecordingFile by remember { mutableStateOf<File?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { activeRecorder?.release() }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { onAttachMediaUri(it, MediaType.PHOTO) } }

    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { onAttachMediaUri(it, MediaType.VIDEO) } }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { onAttachMediaUri(it, MediaType.DOCUMENT) } }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            onAttachMediaUri(uri, MediaType.PHOTO)
        }
    }

    val captureVideoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { success ->
        val uri = pendingVideoUri
        if (success && uri != null) {
            onAttachMediaUri(uri, MediaType.VIDEO)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val (uri, _) = mediaHelper.createCameraOutputUri(isVideo = false)
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } else {
            onPostMessage("Camera permission denied")
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            onRefreshGpsLocation()
        } else {
            showLocationPicker = true
        }
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri != null) {
            val resolved = mediaHelper.resolveContactSelection(uri)
            if (resolved != null) {
                onAttachPerson(resolved.first, resolved.second)
            } else {
                onPostMessage("Could not read contact name")
            }
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                onAppendDictation(spoken)
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to dictate into Hey Bk")
            }
            runCatching { speechLauncher.launch(intent) }.onFailure {
                onPostMessage("Speech recognizer unavailable on this device; use Record Voice instead.")
            }
        } else {
            onPostMessage("Microphone permission is needed for audio dictation")
        }
    }

    val activeTitle = if (isEditing && editorDraft != null) editorDraft.title else selectedEntry?.title.orEmpty()
    val activeTimestamp = if (isEditing && editorDraft != null) editorDraft.entryTimestamp else (selectedEntry?.entryTimestamp ?: System.currentTimeMillis())
    val activeFavorite = if (isEditing && editorDraft != null) editorDraft.isFavorite else (selectedEntry?.isFavorite == true)
    val activeBookId = if (isEditing && editorDraft != null) editorDraft.bookId else selectedEntry?.bookId
    val activeBookTitle = books.firstOrNull { it.id == activeBookId }?.title ?: selectedEntry?.bookTitle ?: "Diary"
    val activeBookColor = parseColorHex(
        books.firstOrNull { it.id == activeBookId }?.colorHex ?: selectedEntry?.bookColorHex ?: "#F5B700",
        palette.accentAmber
    )
    val activeMedia = if (isEditing && editorDraft != null) editorDraft.media else selectedEntry?.media.orEmpty()
    val activeLocationName = if (isEditing && editorDraft != null) editorDraft.locationName else selectedEntry?.locationName
    val activeLat = if (isEditing && editorDraft != null) editorDraft.latitude else selectedEntry?.latitude
    val activeLng = if (isEditing && editorDraft != null) editorDraft.longitude else selectedEntry?.longitude

    val writingFontFamily: FontFamily = if (useSerifWritingFont) LoraFontFamily else PlusJakartaSansFontFamily
    val isKeyboardVisible = WindowInsets.isImeVisible

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.detailPaneBackground)
            .statusBarsPadding()
            .then(if (isEditing) Modifier.imePadding() else Modifier)
    ) {
        // TOP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditing) {
                IconButton(
                    onClick = onSaveDraft,
                    modifier = Modifier.testTag("editor_tick_save")
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = "Save entry", tint = palette.accentBlue)
                }
            } else {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("reader_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
                }
            }

            Text(
                text = headerDateFmt.format(Date(activeTimestamp)),
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                modifier = Modifier.weight(1f)
            )

            if (isEditing && editorDraft != null) {
                IconButton(
                    onClick = onUndo,
                    enabled = editorDraft.canUndo,
                    modifier = Modifier.testTag("editor_undo_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Undo,
                        contentDescription = "Undo",
                        tint = if (editorDraft.canUndo) palette.textPrimary else palette.textMuted
                    )
                }
                IconButton(
                    onClick = onRedo,
                    enabled = editorDraft.canRedo,
                    modifier = Modifier.testTag("editor_redo_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Redo,
                        contentDescription = "Redo",
                        tint = if (editorDraft.canRedo) palette.textPrimary else palette.textMuted
                    )
                }
            } else if (selectedEntry != null) {
                IconButton(
                    onClick = { onStartEditing(selectedEntry) },
                    modifier = Modifier.testTag("reader_edit_button")
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit entry", tint = palette.accentBlue)
                }
            }

            IconButton(
                onClick = {
                    if (isEditing) onToggleInEntrySearch() else readerSearchOpen = !readerSearchOpen
                },
                modifier = Modifier.testTag("entry_search_button")
            ) {
                Icon(Icons.Outlined.Search, contentDescription = "Search in entry", tint = palette.textPrimary)
            }

            IconButton(
                onClick = onToggleFullScreen,
                modifier = Modifier.testTag("entry_fullscreen_button")
            ) {
                Icon(
                    if (isFullScreen) Icons.Rounded.CloseFullscreen else Icons.Rounded.OpenInFull,
                    contentDescription = "Toggle full screen",
                    tint = palette.textPrimary
                )
            }

            Box {
                IconButton(
                    onClick = { showOverflowMenu = true },
                    modifier = Modifier.testTag("entry_more_menu")
                ) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Entry options", tint = palette.textPrimary)
                }
                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (activeFavorite) "Remove Favorite" else "Favorite") },
                        leadingIcon = {
                            Icon(
                                if (activeFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                null,
                                tint = if (activeFavorite) palette.accentAmber else palette.textPrimary
                            )
                        },
                        onClick = {
                            showOverflowMenu = false
                            onToggleFavorite(selectedEntry?.id ?: editorDraft?.entryId, activeFavorite)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Tag & People") },
                        leadingIcon = { Icon(Icons.Outlined.Label, null) },
                        onClick = {
                            showOverflowMenu = false
                            if (!isEditing && selectedEntry != null) onStartEditing(selectedEntry)
                            showTagsPeopleSheet = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Move") },
                        leadingIcon = { Icon(Icons.Outlined.DriveFileMove, null) },
                        onClick = {
                            showOverflowMenu = false
                            showMoveBookDialog = true
                        }
                    )
                    if (selectedEntry != null) {
                        DropdownMenuItem(
                            text = { Text("Copy to") },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                            onClick = {
                                showOverflowMenu = false
                                showCopyBookDialog = true
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Entry Info") },
                        leadingIcon = { Icon(Icons.Outlined.Info, null) },
                        onClick = {
                            showOverflowMenu = false
                            showEntryInfo = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Version History") },
                        leadingIcon = { Icon(Icons.Outlined.History, null) },
                        onClick = {
                            showOverflowMenu = false
                            showVersionHistory = true
                        }
                    )
                    if (selectedEntry != null) {
                        DropdownMenuItem(
                            text = { Text("Export") },
                            leadingIcon = { Icon(Icons.Outlined.IosShare, null) },
                            onClick = {
                                showOverflowMenu = false
                                onExportEntry(selectedEntry)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to Home Screen") },
                            leadingIcon = { Icon(Icons.Outlined.AddToHomeScreen, null) },
                            onClick = {
                                showOverflowMenu = false
                                onPinToHomeScreen(selectedEntry.id, selectedEntry.title)
                            }
                        )
                    }
                    val trashTargetId = selectedEntry?.id ?: editorDraft?.entryId
                    if (trashTargetId != null) {
                        DropdownMenuItem(
                            text = { Text("Trash", color = palette.accentCoral) },
                            leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null, tint = palette.accentCoral) },
                            onClick = {
                                showOverflowMenu = false
                                onTrashEntry(trashTargetId)
                            }
                        )
                    }
                }
            }
        }

        // Inline Find Bar
        val showFindBar = if (isEditing) editorDraft?.inEntrySearchVisible == true else readerSearchOpen
        val currentFindQuery = if (isEditing) editorDraft?.inEntrySearchQuery.orEmpty() else readerSearchQuery
        if (showFindBar) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = currentFindQuery,
                    onValueChange = {
                        if (isEditing) onUpdateInEntrySearch(it) else readerSearchQuery = it
                    },
                    placeholder = { Text("Find inside entry...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    if (isEditing) onToggleInEntrySearch() else {
                        readerSearchOpen = false
                        readerSearchQuery = ""
                    }
                }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close find")
                }
            }
        }

        // BODY SCROLL AREA
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // Editor Title or Reader Title
            if (isEditing && editorDraft != null) {
                val titleFocus = remember { FocusRequester() }
                LaunchedEffect(editorDraft.entryId) {
                    if (editorDraft.entryId == null && editorDraft.title.isEmpty()) {
                        runCatching { titleFocus.requestFocus() }
                    }
                }
                BasicTextField(
                    value = editorDraft.title,
                    onValueChange = onUpdateTitle,
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        color = palette.textPrimary,
                        fontFamily = writingFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    cursorBrush = SolidColor(palette.accentBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(titleFocus)
                        .testTag("editor_title_input"),
                    decorationBox = { inner ->
                        if (editorDraft.title.isEmpty()) {
                            Text(
                                text = "Entry title...",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = palette.textMuted,
                                    fontFamily = writingFontFamily
                                )
                            )
                        }
                        inner()
                    }
                )
            } else {
                Text(
                    text = activeTitle,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = palette.textPrimary,
                        fontFamily = writingFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(Modifier.height(10.dp))

            // Attached Photos & Media strip
            if (activeMedia.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    activeMedia.forEach { att ->
                        when (att.type) {
                            MediaType.PHOTO -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { zoomedPhotoUri = att.localUri }
                                ) {
                                    AsyncImage(
                                        model = att.localUri,
                                        contentDescription = att.displayName,
                                        contentScale = ContentScale.FillWidth,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    if (isEditing) {
                                        IconButton(
                                            onClick = { onRemoveMedia(att.id) },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.6f))
                                        ) {
                                            Icon(Icons.Rounded.Close, "Remove photo", tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                            MediaType.VIDEO, MediaType.AUDIO, MediaType.DOCUMENT -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(palette.cardSurface)
                                        .border(1.dp, palette.hairline, RoundedCornerShape(12.dp))
                                        .clickable {
                                            when (att.type) {
                                                MediaType.VIDEO -> playingVideoUri = att.localUri
                                                MediaType.AUDIO -> playingAudio = att
                                                else -> {}
                                            }
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (att.type) {
                                            MediaType.VIDEO -> Icons.Outlined.Videocam
                                            MediaType.AUDIO -> Icons.Outlined.GraphicEq
                                            else -> Icons.Outlined.AttachFile
                                        },
                                        contentDescription = null,
                                        tint = palette.accentBlue
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = att.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isEditing) {
                                        IconButton(onClick = { onRemoveMedia(att.id) }) {
                                            Icon(Icons.Rounded.Close, "Remove attachment", tint = palette.textSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // Rich Blocks Content
            if (isEditing && editorDraft != null) {
                var numCounter = 1
                editorDraft.blocks.forEachIndexed { idx, block ->
                    if (block.type != BlockType.NUMBERED_LIST) numCounter = 1
                    val listNum = if (block.type == BlockType.NUMBERED_LIST) numCounter++ else 0
                    EditableRichBlockRow(
                        block = block,
                        blockIndex = idx,
                        listNumber = listNum,
                        fontFamily = writingFontFamily,
                        searchQuery = editorDraft.inEntrySearchQuery,
                        onFocusChanged = { s, e -> onFocusBlock(idx, s, e) },
                        onTextChanged = { onUpdateBlockText(idx, it) },
                        onBackspaceWhenEmpty = { onBackspaceEmptyBlock(idx) },
                        onToggleChecklist = { onToggleChecklist(idx) }
                    )
                }
            } else if (selectedEntry != null) {
                val doc = remember(selectedEntry.richContentJson) {
                    RichDocument.decode(selectedEntry.richContentJson)
                }
                var numCounter = 1
                doc.blocks.forEach { block ->
                    if (block.type != BlockType.NUMBERED_LIST) numCounter = 1
                    val listNum = if (block.type == BlockType.NUMBERED_LIST) numCounter++ else 0
                    ReadOnlyRichBlockRow(
                        block = block,
                        listNumber = listNum,
                        fontFamily = writingFontFamily,
                        searchQuery = readerSearchQuery,
                        onOpenUrl = { url -> runCatching { uriHandler.openUri(url) } }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Metadata footer: Book, Time, Location, People, Tags
            val currentTags = if (isEditing && editorDraft != null) editorDraft.tagNames else selectedEntry?.tags?.map { it.name }.orEmpty()
            val currentPeople = if (isEditing && editorDraft != null) editorDraft.people else selectedEntry?.people.orEmpty()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = timeFmt.format(Date(activeTimestamp)),
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textSecondary
                )
                Text(
                    text = activeBookTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = activeBookColor,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { showMoveBookDialog = true }
                )
                if (editorDraft?.isCapturingLocation == true) {
                    Text(
                        text = "• Locating...",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.accentBlue
                    )
                } else if (!activeLocationName.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { if (isEditing) showLocationPicker = true }
                    ) {
                        Icon(Icons.Outlined.LocationOn, null, tint = palette.textSecondary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = activeLocationName,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                }
            }

            if (currentPeople.isNotEmpty() || currentTags.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currentPeople.forEach { person ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(palette.accentAmber.copy(alpha = 0.16f))
                                .clickable {
                                    if (!isEditing) onTapPersonChip(person.id) else showTagsPeopleSheet = true
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.PersonOutline, null, tint = palette.accentAmber, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(person.displayName, style = MaterialTheme.typography.labelSmall, color = palette.accentAmber)
                        }
                    }
                    currentTags.forEach { tagName ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(palette.selectedCard)
                                .clickable {
                                    if (!isEditing) {
                                        allTags.firstOrNull { it.name.equals(tagName, ignoreCase = true) }
                                            ?.let { onTapTagChip(it.id) }
                                    } else showTagsPeopleSheet = true
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#$tagName", style = MaterialTheme.typography.labelSmall, color = palette.accentBlue)
                        }
                    }
                }
            }

            Spacer(Modifier.height(36.dp))
        }

        // CONTEXTUAL EDITOR BOTTOM TOOLBAR
        if (isEditing && editorDraft != null) {
            HorizontalDivider(color = palette.hairline)
            if (isKeyboardVisible) {
                // ABOVE KEYBOARD: Formatting + Photos, Templates, AI
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.masterPaneBackground)
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FormatChip("H1", editorDraft.blocks.getOrNull(editorDraft.focusedBlockIndex)?.type == BlockType.HEADING_1) {
                            onSetBlockType(BlockType.HEADING_1)
                        }
                        FormatChip("H2", editorDraft.blocks.getOrNull(editorDraft.focusedBlockIndex)?.type == BlockType.HEADING_2) {
                            onSetBlockType(BlockType.HEADING_2)
                        }
                        FormatChip("Txt", editorDraft.blocks.getOrNull(editorDraft.focusedBlockIndex)?.type == BlockType.NORMAL) {
                            onSetBlockType(BlockType.NORMAL)
                        }
                        FormatChip(if (useSerifWritingFont) "Serif" else "Sans", useSerifWritingFont) {
                            useSerifWritingFont = !useSerifWritingFont
                        }
                        IconButton(onClick = { onSetBlockType(BlockType.QUOTE) }) {
                            Icon(Icons.Outlined.FormatQuote, "Quote", tint = palette.textPrimary)
                        }
                        IconButton(onClick = { onSetBlockType(BlockType.BULLET_LIST) }) {
                            Icon(Icons.AutoMirrored.Rounded.FormatListBulleted, "Bullet list", tint = palette.textPrimary)
                        }
                        IconButton(onClick = { onSetBlockType(BlockType.NUMBERED_LIST) }) {
                            Icon(Icons.Outlined.FormatListNumbered, "Numbered list", tint = palette.textPrimary)
                        }
                        IconButton(onClick = { onSetBlockType(BlockType.CHECKLIST) }) {
                            Icon(Icons.Outlined.CheckBox, "Checklist", tint = palette.textPrimary)
                        }
                        IconButton(onClick = { onToggleInlineMark(InlineMarkType.BOLD, null) }) {
                            Icon(Icons.Outlined.FormatBold, "Bold", tint = palette.textPrimary)
                        }
                        IconButton(onClick = { onToggleInlineMark(InlineMarkType.ITALIC, null) }) {
                            Icon(Icons.Outlined.FormatItalic, "Italic", tint = palette.textPrimary)
                        }
                        IconButton(onClick = { onToggleInlineMark(InlineMarkType.UNDERLINE, null) }) {
                            Icon(Icons.Outlined.FormatUnderlined, "Underline", tint = palette.textPrimary)
                        }
                        IconButton(onClick = { onToggleInlineMark(InlineMarkType.STRIKETHROUGH, null) }) {
                            Icon(Icons.Outlined.FormatStrikethrough, "Strikethrough", tint = palette.textPrimary)
                        }
                        IconButton(onClick = { showHyperlinkDialog = true }) {
                            Icon(Icons.Outlined.Link, "Hyperlink", tint = palette.textPrimary)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BottomPillButton("Photos", Icons.Outlined.Image, palette.textPrimary) {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                        BottomPillButton("Templates", Icons.Outlined.NoteAdd, palette.textPrimary) {
                            showTemplatesSheet = true
                        }
                        BottomPillButton("AI", Icons.Outlined.AutoAwesome, palette.accentBlue) {
                            showAiDialog = true
                        }
                    }
                }
            } else {
                // NO KEYBOARD: AI, Audio dictation, Location, Tag, Camera, File, Video (+ overflow 3-dot if tight)
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.masterPaneBackground)
                        .navigationBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    val slotWidth = 52.dp
                    val maxVisible = ((maxWidth - 52.dp) / slotWidth).toInt().coerceIn(3, 7)

                    val actions = listOf(
                        ToolbarActionItem("AI", Icons.Outlined.AutoAwesome, palette.accentBlue) {
                            showAiDialog = true
                        },
                        ToolbarActionItem("Dictate", Icons.Outlined.Mic, palette.textPrimary) {
                            val granted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to dictate into Hey Bk")
                                }
                                runCatching { speechLauncher.launch(intent) }
                            } else {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        ToolbarActionItem("Location", Icons.Outlined.LocationOn, palette.textPrimary) {
                            if (activeLat == null || activeLng == null) {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                )
                            } else {
                                showLocationPicker = true
                            }
                        },
                        ToolbarActionItem("Tag", Icons.Outlined.Label, palette.textPrimary) {
                            showTagsPeopleSheet = true
                        },
                        ToolbarActionItem("Camera", Icons.Outlined.CameraAlt, palette.textPrimary) {
                            val granted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                val (uri, _) = mediaHelper.createCameraOutputUri(isVideo = false)
                                pendingCameraUri = uri
                                takePictureLauncher.launch(uri)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        ToolbarActionItem("File", Icons.Outlined.AttachFile, palette.textPrimary) {
                            filePicker.launch(arrayOf("*/*"))
                        },
                        ToolbarActionItem("Video", Icons.Outlined.Videocam, palette.textPrimary) {
                            videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        }
                    )

                    val visibleActions = actions.take(maxVisible)
                    val overflowActions = actions.drop(maxVisible)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        visibleActions.forEach { act ->
                            IconButton(
                                onClick = act.onClick,
                                modifier = Modifier.testTag("editor_bottom_${act.label.lowercase()}")
                            ) {
                                Icon(act.icon, contentDescription = act.label, tint = act.tint)
                            }
                        }
                        Box {
                            IconButton(
                                onClick = { showBottomOverflow = true },
                                modifier = Modifier.testTag("editor_bottom_overflow")
                            ) {
                                Icon(Icons.Rounded.MoreHoriz, contentDescription = "More tools", tint = palette.textPrimary)
                            }
                            DropdownMenu(
                                expanded = showBottomOverflow,
                                onDismissRequest = { showBottomOverflow = false }
                            ) {
                                overflowActions.forEach { act ->
                                    DropdownMenuItem(
                                        text = { Text(act.label) },
                                        leadingIcon = { Icon(act.icon, null, tint = act.tint) },
                                        onClick = {
                                            showBottomOverflow = false
                                            act.onClick()
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Photos") },
                                    leadingIcon = { Icon(Icons.Outlined.Image, null) },
                                    onClick = {
                                        showBottomOverflow = false
                                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Templates") },
                                    leadingIcon = { Icon(Icons.Outlined.NoteAdd, null) },
                                    onClick = {
                                        showBottomOverflow = false
                                        showTemplatesSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Pick Contact / People") },
                                    leadingIcon = { Icon(Icons.Outlined.PersonOutline, null) },
                                    onClick = {
                                        showBottomOverflow = false
                                        showTagsPeopleSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(if (activeRecorder == null) "Record Voice Clip" else "Stop Voice Recording")
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (activeRecorder == null) Icons.Outlined.GraphicEq else Icons.Outlined.Stop,
                                            null
                                        )
                                    },
                                    onClick = {
                                        showBottomOverflow = false
                                        if (activeRecorder == null) {
                                            val granted = ContextCompat.checkSelfPermission(
                                                context, Manifest.permission.RECORD_AUDIO
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (!granted) {
                                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            } else {
                                                val outFile = mediaHelper.createAudioRecordingFile()
                                                val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                    MediaRecorder(context)
                                                } else {
                                                    @Suppress("DEPRECATION")
                                                    MediaRecorder()
                                                }
                                                runCatching {
                                                    rec.setAudioSource(MediaRecorder.AudioSource.MIC)
                                                    rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                                    rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                                    rec.setOutputFile(outFile.absolutePath)
                                                    rec.prepare()
                                                    rec.start()
                                                    activeRecorder = rec
                                                    activeRecordingFile = outFile
                                                    onPostMessage("Recording audio... open ... menu to stop & attach")
                                                }.onFailure {
                                                    onPostMessage("Unable to start audio recorder")
                                                }
                                            }
                                        } else {
                                            runCatching {
                                                activeRecorder?.stop()
                                                activeRecorder?.release()
                                            }
                                            activeRecorder = null
                                            activeRecordingFile?.let { f ->
                                                onAttachRecordedAudio(Uri.fromFile(f).toString(), f.name)
                                            }
                                            activeRecordingFile = null
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Record Video") },
                                    leadingIcon = { Icon(Icons.Outlined.Videocam, null) },
                                    onClick = {
                                        showBottomOverflow = false
                                        val (uri, _) = mediaHelper.createCameraOutputUri(isVideo = true)
                                        pendingVideoUri = uri
                                        captureVideoLauncher.launch(uri)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs & Sheets
    if (showTagsPeopleSheet && editorDraft != null) {
        TagAndPeopleSheet(
            currentTags = editorDraft.tagNames,
            allTags = allTags,
            currentPeople = editorDraft.people,
            allPeople = allPeople,
            onAddTag = onAddTag,
            onRemoveTag = onRemoveTag,
            onPickFromContacts = { contactPickerLauncher.launch(null) },
            onAddOrAttachPerson = { name -> onAttachPerson(name, null) },
            onRemovePerson = onRemovePerson,
            onDismiss = { showTagsPeopleSheet = false }
        )
    }

    if (showTemplatesSheet) {
        TemplatesBottomSheet(
            onSelectTemplate = onApplyTemplate,
            onDismiss = { showTemplatesSheet = false }
        )
    }

    if (showVersionHistory) {
        VersionHistoryBottomSheet(
            versions = versions,
            onRestoreVersion = { ver ->
                if (!isEditing && selectedEntry != null) onStartEditing(selectedEntry)
                onRestoreVersion(ver)
            },
            onDismiss = { showVersionHistory = false }
        )
    }

    if (showMoveBookDialog) {
        BookPickerDialog(
            title = "Move to Journal Book",
            books = books,
            currentBookId = activeBookId,
            onSelectBook = { book ->
                onMoveToBook(selectedEntry?.id ?: editorDraft?.entryId, book.id)
            },
            onDismiss = { showMoveBookDialog = false }
        )
    }

    if (showCopyBookDialog && selectedEntry != null) {
        BookPickerDialog(
            title = "Copy Entry to Book",
            books = books,
            currentBookId = activeBookId,
            onSelectBook = { book ->
                onCopyToBook(selectedEntry.id, book.id)
            },
            onDismiss = { showCopyBookDialog = false }
        )
    }

    if (showEntryInfo) {
        val doc = if (isEditing && editorDraft != null) {
            editorDraft.toDocument()
        } else {
            RichDocument.decode(selectedEntry?.richContentJson.orEmpty())
        }
        EntryInfoDialog(
            title = activeTitle,
            bookTitle = activeBookTitle,
            document = doc,
            createdTimestamp = activeTimestamp,
            updatedTimestamp = selectedEntry?.updatedAt ?: editorDraft?.updatedAt ?: activeTimestamp,
            locationName = activeLocationName,
            latitude = activeLat,
            longitude = activeLng,
            mediaCount = activeMedia.size,
            onDismiss = { showEntryInfo = false }
        )
    }

    if (showHyperlinkDialog) {
        HyperlinkDialog(
            onDismiss = { showHyperlinkDialog = false },
            onConfirm = { url -> onToggleInlineMark(InlineMarkType.LINK, url) }
        )
    }

    if (showLocationPicker) {
        LocationPickerDialog(
            initialLat = activeLat,
            initialLng = activeLng,
            onDismiss = { showLocationPicker = false },
            onPickLocation = onPickCustomLocation,
            onUseCurrentGps = {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
            }
        )
    }

    if (showAiDialog) {
        AiComingSoonDialog(onDismiss = { showAiDialog = false })
    }

    if (zoomedPhotoUri != null) {
        CleanPhotoZoomDialog(uri = zoomedPhotoUri!!, onDismiss = { zoomedPhotoUri = null })
    }

    if (playingVideoUri != null) {
        VideoPlayerDialog(uri = playingVideoUri!!, onDismiss = { playingVideoUri = null })
    }

    if (playingAudio != null) {
        AudioPlayerDialog(attachment = playingAudio!!, onDismiss = { playingAudio = null })
    }
}

private data class ToolbarActionItem(
    val label: String,
    val icon: ImageVector,
    val tint: Color,
    val onClick: () -> Unit
)

@Composable
private fun FormatChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalHeyBkPalette.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) palette.selectedCard else palette.cardSurface)
            .border(1.dp, if (active) palette.accentBlue else palette.hairline, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) palette.accentBlue else palette.textPrimary
        )
    }
}

@Composable
private fun BottomPillButton(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    val palette = LocalHeyBkPalette.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(palette.cardSurface)
            .border(1.dp, palette.hairline, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

@Composable
private fun EditableRichBlockRow(
    block: RichBlock,
    blockIndex: Int,
    listNumber: Int,
    fontFamily: FontFamily,
    searchQuery: String,
    onFocusChanged: (Int, Int) -> Unit,
    onTextChanged: (String) -> Unit,
    onBackspaceWhenEmpty: () -> Unit,
    onToggleChecklist: () -> Unit
) {
    val palette = LocalHeyBkPalette.current
    val baseStyle = blockTextStyle(block.type, fontFamily, palette.textPrimary, block.checked)
    val styledAnnotated = remember(block.text, block.spans, searchQuery, palette.accentBlue) {
        block.toAnnotatedString(linkColor = palette.accentBlue, searchQuery = searchQuery)
    }
    var tfv by remember(block.id) { mutableStateOf(TextFieldValue(block.text)) }
    if (tfv.text != block.text) {
        tfv = tfv.copy(text = block.text)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .then(
                if (block.type == BlockType.QUOTE) {
                    Modifier
                        .background(palette.cardSurface.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .border(1.dp, palette.accentBlue.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                } else Modifier
            ),
        verticalAlignment = Alignment.Top
    ) {
        when (block.type) {
            BlockType.BULLET_LIST -> {
                Text(
                    text = "•",
                    style = baseStyle,
                    color = palette.accentBlue,
                    modifier = Modifier.padding(end = 10.dp)
                )
            }
            BlockType.NUMBERED_LIST -> {
                Text(
                    text = "$listNumber.",
                    style = baseStyle,
                    color = palette.accentBlue,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            BlockType.CHECKLIST -> {
                Checkbox(
                    checked = block.checked,
                    onCheckedChange = { onToggleChecklist() },
                    modifier = Modifier.size(24.dp).padding(end = 8.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            else -> {}
        }

        BasicTextField(
            value = tfv,
            onValueChange = { next ->
                tfv = next
                onFocusChanged(next.selection.start, next.selection.end)
                if (next.text != block.text) {
                    onTextChanged(next.text)
                }
            },
            textStyle = baseStyle,
            cursorBrush = SolidColor(palette.accentBlue),
            visualTransformation = { _ ->
                TransformedText(styledAnnotated, OffsetMapping.Identity)
            },
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        onFocusChanged(tfv.selection.start, tfv.selection.end)
                    }
                }
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown &&
                        keyEvent.key == Key.Backspace &&
                        block.text.isEmpty() &&
                        blockIndex > 0
                    ) {
                        onBackspaceWhenEmpty()
                        true
                    } else false
                }
                .testTag("editor_block_$blockIndex"),
            decorationBox = { inner ->
                if (block.text.isEmpty() && blockIndex == 0) {
                    Text(
                        text = "Start writing...",
                        style = baseStyle.copy(color = palette.textMuted)
                    )
                }
                inner()
            }
        )
    }
}

@Suppress("DEPRECATION")
@Composable
private fun ReadOnlyRichBlockRow(
    block: RichBlock,
    listNumber: Int,
    fontFamily: FontFamily,
    searchQuery: String,
    onOpenUrl: (String) -> Unit
) {
    val palette = LocalHeyBkPalette.current
    val baseStyle = blockTextStyle(block.type, fontFamily, palette.textPrimary, block.checked)
    val annotated = remember(block.text, block.spans, searchQuery, palette.accentBlue) {
        block.toAnnotatedString(linkColor = palette.accentBlue, searchQuery = searchQuery)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .then(
                if (block.type == BlockType.QUOTE) {
                    Modifier
                        .background(palette.cardSurface, RoundedCornerShape(8.dp))
                        .border(1.dp, palette.accentBlue.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                } else Modifier
            ),
        verticalAlignment = Alignment.Top
    ) {
        when (block.type) {
            BlockType.BULLET_LIST -> Text("•", style = baseStyle, color = palette.accentBlue, modifier = Modifier.padding(end = 10.dp))
            BlockType.NUMBERED_LIST -> Text("$listNumber.", style = baseStyle, color = palette.accentBlue, modifier = Modifier.padding(end = 8.dp))
            BlockType.CHECKLIST -> Text(
                if (block.checked) "☑" else "☐",
                style = baseStyle,
                color = if (block.checked) palette.accentSage else palette.textSecondary,
                modifier = Modifier.padding(end = 8.dp)
            )
            else -> {}
        }

        ClickableText(
            text = annotated,
            style = baseStyle,
            onClick = { offset ->
                annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()
                    ?.let { onOpenUrl(it.item) }
            },
            modifier = Modifier.weight(1f)
        )
    }
}

private fun blockTextStyle(
    type: BlockType,
    fontFamily: FontFamily,
    textColor: Color,
    checked: Boolean
): TextStyle {
    return when (type) {
        BlockType.HEADING_1 -> TextStyle(
            fontFamily = LoraFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            color = textColor
        )
        BlockType.HEADING_2 -> TextStyle(
            fontFamily = LoraFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 19.sp,
            lineHeight = 26.sp,
            color = textColor
        )
        BlockType.QUOTE -> TextStyle(
            fontFamily = fontFamily,
            fontStyle = FontStyle.Italic,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = textColor.copy(alpha = 0.9f)
        )
        BlockType.CHECKLIST -> TextStyle(
            fontFamily = fontFamily,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
            color = if (checked) textColor.copy(alpha = 0.55f) else textColor
        )
        else -> TextStyle(
            fontFamily = fontFamily,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = textColor
        )
    }
}
