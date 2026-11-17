package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.JournalBook
import com.example.domain.model.MainSection
import com.example.ui.theme.BookAllEntries
import com.example.ui.theme.BookPresetColors
import com.example.ui.theme.LocalHeyBkPalette
import com.example.ui.theme.parseColorHex

@Composable
fun TabletNavigationRail(
    section: MainSection,
    activeBookId: String?,
    books: List<JournalBook>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelectSection: (MainSection) -> Unit,
    onSelectBook: (String?) -> Unit,
    onOpenNewBookDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalHeyBkPalette.current
    var showMoreMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .width(if (isExpanded) 196.dp else 78.dp)
            .fillMaxHeight()
            .background(palette.railBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .animateContentSize()
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = if (isExpanded) Alignment.Start else Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onToggleExpand,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("rail_menu_toggle")
        ) {
            Icon(
                imageVector = Icons.Outlined.Menu,
                contentDescription = "Toggle navigation labels",
                tint = palette.textPrimary
            )
        }

        Spacer(Modifier.height(6.dp))

        RailPrimaryItem(
            icon = Icons.Outlined.Today,
            label = "Today",
            selected = section == MainSection.TODAY,
            expanded = isExpanded,
            testTag = "rail_today",
            onClick = { onSelectSection(MainSection.TODAY) }
        )

        RailPrimaryItem(
            icon = Icons.Outlined.FormatQuote,
            label = "Prompts",
            selected = section == MainSection.PROMPTS,
            expanded = isExpanded,
            testTag = "rail_prompts",
            onClick = { onSelectSection(MainSection.PROMPTS) }
        )

        Box {
            RailPrimaryItem(
                icon = Icons.Outlined.MoreHoriz,
                label = "More",
                selected = section in listOf(
                    MainSection.PEOPLE,
                    MainSection.TAGS,
                    MainSection.TRASH,
                    MainSection.SETTINGS
                ),
                expanded = isExpanded,
                testTag = "rail_more",
                onClick = { showMoreMenu = true }
            )
            DropdownMenu(
                expanded = showMoreMenu,
                onDismissRequest = { showMoreMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("People & Contacts") },
                    leadingIcon = { Icon(Icons.Outlined.PeopleOutline, null) },
                    onClick = {
                        showMoreMenu = false
                        onSelectSection(MainSection.PEOPLE)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Tags") },
                    leadingIcon = { Icon(Icons.Outlined.Label, null) },
                    onClick = {
                        showMoreMenu = false
                        onSelectSection(MainSection.TAGS)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Trash") },
                    leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) },
                    onClick = {
                        showMoreMenu = false
                        onSelectSection(MainSection.TRASH)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Settings & Backup") },
                    leadingIcon = { Icon(Icons.Outlined.Settings, null) },
                    onClick = {
                        showMoreMenu = false
                        onSelectSection(MainSection.SETTINGS)
                    }
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
            color = palette.hairline
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = if (isExpanded) Alignment.Start else Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            RailBookItem(
                title = "All Entries",
                color = BookAllEntries,
                isAllEntries = true,
                selected = section == MainSection.BOOK_FEED && activeBookId == null,
                expanded = isExpanded,
                testTag = "rail_all_entries",
                onClick = { onSelectBook(null) }
            )

            books.forEach { book ->
                RailBookItem(
                    title = book.title,
                    color = parseColorHex(book.colorHex),
                    isAllEntries = false,
                    selected = section == MainSection.BOOK_FEED && activeBookId == book.id,
                    expanded = isExpanded,
                    testTag = "rail_book_${book.id}",
                    onClick = { onSelectBook(book.id) }
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onOpenNewBookDialog)
                .minimumInteractiveComponentSize()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .testTag("rail_add_book"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "New journal book",
                tint = palette.accentBlue,
                modifier = Modifier.size(20.dp)
            )
            if (isExpanded) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "New Book",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.accentBlue
                )
            }
        }
    }
}

@Composable
private fun RailPrimaryItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    expanded: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val palette = LocalHeyBkPalette.current
    val bg = if (selected) palette.selectedCard else Color.Transparent
    val fg = if (selected) palette.textPrimary else palette.textSecondary

    if (expanded) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(bg)
                .clickable(onClick = onClick)
                .minimumInteractiveComponentSize()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag(testTag),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 6.dp)
                .testTag(testTag),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RailBookItem(
    title: String,
    color: Color,
    isAllEntries: Boolean,
    selected: Boolean,
    expanded: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val palette = LocalHeyBkPalette.current
    val bg = if (selected) palette.elevatedTile else Color.Transparent
    val textColor = if (selected) palette.textPrimary else palette.textSecondary

    if (expanded) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(bg)
                .clickable(onClick = onClick)
                .minimumInteractiveComponentSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag(testTag),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BookCoverGlyph(color = color, isAllEntries = isAllEntries, width = 16.dp, height = 22.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 5.dp, horizontal = 2.dp)
                .testTag(testTag),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                BookCoverGlyph(color = color, isAllEntries = isAllEntries, width = 15.dp, height = 20.dp)
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                color = textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun BookCoverGlyph(
    color: Color,
    isAllEntries: Boolean = false,
    width: Dp = 18.dp,
    height: Dp = 24.dp
) {
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isAllEntries) color.copy(alpha = 0.85f) else color)
    ) {
        // Subtle spine line like Day One book icons
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(3.dp)
                .background(Color.Black.copy(alpha = 0.22f))
        )
    }
}

@Composable
fun PhoneBottomNavigationBar(
    section: MainSection,
    onSelectSection: (MainSection) -> Unit
) {
    val palette = LocalHeyBkPalette.current
    NavigationBar(
        containerColor = palette.railBackground,
        contentColor = palette.textPrimary
    ) {
        NavigationBarItem(
            selected = section == MainSection.TODAY,
            onClick = { onSelectSection(MainSection.TODAY) },
            icon = { Icon(Icons.Outlined.Today, contentDescription = "Today") },
            label = { Text("Today", maxLines = 1) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = palette.accentBlue,
                selectedTextColor = palette.accentBlue,
                indicatorColor = palette.selectedCard,
                unselectedIconColor = palette.textSecondary,
                unselectedTextColor = palette.textSecondary
            ),
            modifier = Modifier.testTag("bottom_nav_today")
        )
        NavigationBarItem(
            selected = section == MainSection.BOOK_FEED,
            onClick = { onSelectSection(MainSection.BOOK_FEED) },
            icon = { Icon(Icons.Rounded.MenuBook, contentDescription = "Journals") },
            label = { Text("Entries", maxLines = 1) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = palette.accentBlue,
                selectedTextColor = palette.accentBlue,
                indicatorColor = palette.selectedCard,
                unselectedIconColor = palette.textSecondary,
                unselectedTextColor = palette.textSecondary
            ),
            modifier = Modifier.testTag("bottom_nav_entries")
        )
        NavigationBarItem(
            selected = section == MainSection.PROMPTS,
            onClick = { onSelectSection(MainSection.PROMPTS) },
            icon = { Icon(Icons.Outlined.FormatQuote, contentDescription = "Prompts") },
            label = { Text("Prompts", maxLines = 1) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = palette.accentBlue,
                selectedTextColor = palette.accentBlue,
                indicatorColor = palette.selectedCard,
                unselectedIconColor = palette.textSecondary,
                unselectedTextColor = palette.textSecondary
            ),
            modifier = Modifier.testTag("bottom_nav_prompts")
        )
        NavigationBarItem(
            selected = section == MainSection.PEOPLE,
            onClick = { onSelectSection(MainSection.PEOPLE) },
            icon = { Icon(Icons.Outlined.PeopleOutline, contentDescription = "People") },
            label = { Text("People", maxLines = 1) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = palette.accentBlue,
                selectedTextColor = palette.accentBlue,
                indicatorColor = palette.selectedCard,
                unselectedIconColor = palette.textSecondary,
                unselectedTextColor = palette.textSecondary
            ),
            modifier = Modifier.testTag("bottom_nav_people")
        )
        NavigationBarItem(
            selected = section in listOf(MainSection.TAGS, MainSection.TRASH, MainSection.SETTINGS),
            onClick = { onSelectSection(MainSection.SETTINGS) },
            icon = { Icon(Icons.Outlined.MoreHoriz, contentDescription = "More") },
            label = { Text("More", maxLines = 1) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = palette.accentBlue,
                selectedTextColor = palette.accentBlue,
                indicatorColor = palette.selectedCard,
                unselectedIconColor = palette.textSecondary,
                unselectedTextColor = palette.textSecondary
            ),
            modifier = Modifier.testTag("bottom_nav_more")
        )
    }
}

@Composable
fun BooksDrawerContent(
    activeBookId: String?,
    books: List<JournalBook>,
    onSelectBook: (String?) -> Unit,
    onSelectSection: (MainSection) -> Unit,
    onCreateBook: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    val palette = LocalHeyBkPalette.current
    Column(
        modifier = Modifier
            .width(288.dp)
            .fillMaxHeight()
            .background(palette.railBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hey Bk",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            IconButton(onClick = onCloseDrawer) {
                Icon(Icons.Rounded.Close, contentDescription = "Close books menu", tint = palette.textSecondary)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("JOURNAL BOOKS", style = MaterialTheme.typography.labelSmall, color = palette.textMuted)
        Spacer(Modifier.height(8.dp))

        DrawerRowItem(
            title = "All Entries",
            color = BookAllEntries,
            selected = activeBookId == null,
            onClick = {
                onSelectBook(null)
                onCloseDrawer()
            }
        )
        books.forEach { book ->
            DrawerRowItem(
                title = book.title,
                color = parseColorHex(book.colorHex),
                selected = activeBookId == book.id,
                onClick = {
                    onSelectBook(book.id)
                    onCloseDrawer()
                }
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    onCloseDrawer()
                    onCreateBook()
                }
                .minimumInteractiveComponentSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = palette.accentBlue)
            Spacer(Modifier.width(12.dp))
            Text("Create Journal Book", color = palette.accentBlue, style = MaterialTheme.typography.labelLarge)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = palette.hairline)
        Text("WORKSPACE", style = MaterialTheme.typography.labelSmall, color = palette.textMuted)
        Spacer(Modifier.height(8.dp))

        SimpleDrawerSectionRow("Tags", Icons.Outlined.Label) {
            onSelectSection(MainSection.TAGS)
            onCloseDrawer()
        }
        SimpleDrawerSectionRow("People & Contacts", Icons.Outlined.PeopleOutline) {
            onSelectSection(MainSection.PEOPLE)
            onCloseDrawer()
        }
        SimpleDrawerSectionRow("Trash", Icons.Outlined.DeleteOutline) {
            onSelectSection(MainSection.TRASH)
            onCloseDrawer()
        }
        SimpleDrawerSectionRow("Settings & Backup", Icons.Outlined.Settings) {
            onSelectSection(MainSection.SETTINGS)
            onCloseDrawer()
        }
    }
}

@Composable
private fun DrawerRowItem(
    title: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalHeyBkPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) palette.selectedCard else Color.Transparent)
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BookCoverGlyph(color = color, width = 16.dp, height = 22.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = palette.textPrimary
        )
    }
}

@Composable
private fun SimpleDrawerSectionRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val palette = LocalHeyBkPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = palette.textSecondary)
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary)
    }
}

@Composable
fun CreateBookDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, colorHex: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(BookPresetColors.first()) }
    val palette = LocalHeyBkPalette.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Journal Book") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Book name (e.g. Travel, Gratitude)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_book_name_input")
                )
                Text("Cover color", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BookPresetColors.forEach { hex ->
                        val c = parseColorHex(hex)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(c)
                                .then(
                                    if (selectedColor == hex) {
                                        Modifier.border(2.5.dp, palette.textPrimary, RoundedCornerShape(8.dp))
                                    } else Modifier
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, selectedColor) },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_book_button")
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
