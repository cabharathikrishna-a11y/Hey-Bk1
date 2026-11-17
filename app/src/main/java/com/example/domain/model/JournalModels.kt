package com.example.domain.model

import kotlinx.serialization.Serializable

enum class MainSection {
    TODAY,
    BOOK_FEED,
    PROMPTS,
    PEOPLE,
    TAGS,
    TRASH,
    SETTINGS
}

enum class FeedTab(val label: String) {
    LIST("List"),
    CALENDAR("Calendar"),
    MEDIA("Media"),
    MAP("Map")
}

enum class MediaFilter(val label: String) {
    ALL("All"),
    PHOTOS("Photos"),
    VIDEO("Video"),
    AUDIO("Audio"),
    FILES("Files"),
    MAPS("Maps")
}

enum class PromptCategory(val title: String) {
    REFLECTION("Reflection"),
    GRATITUDE_ABOUT_ME("Gratitude about me"),
    MINDFULNESS("Mindfulness"),
    FUN("Fun"),
    FITNESS("Fitness"),
    CREATIVITY("Creativity"),
    LOVE("Love"),
    LEGACY("Legacy"),
    THE_YEAR_AHEAD("The Year Ahead")
}

@Serializable
enum class MediaType {
    PHOTO,
    VIDEO,
    AUDIO,
    DOCUMENT
}

@Serializable
data class JournalBook(
    val id: String,
    val title: String,
    val colorHex: String,
    val iconName: String = "book",
    val createdAt: Long,
    val isDefault: Boolean = false
)

@Serializable
data class MediaAttachment(
    val id: String,
    val entryId: String,
    val type: MediaType,
    val localUri: String,
    val displayName: String,
    val durationMs: Long = 0L,
    val createdAt: Long
)

@Serializable
data class Tag(
    val id: String,
    val name: String
)

@Serializable
data class Person(
    val id: String,
    val displayName: String,
    val contactLookupUri: String? = null,
    val note: String = ""
)

@Serializable
data class EntryVersion(
    val id: String,
    val entryId: String,
    val titleSnapshot: String,
    val richContentJson: String,
    val savedAt: Long,
    val summaryLabel: String
)

@Serializable
data class JournalPrompt(
    val id: String,
    val category: String,
    val promptText: String,
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false
)

data class JournalEntryDetail(
    val id: String,
    val bookId: String,
    val bookTitle: String,
    val bookColorHex: String,
    val title: String,
    val richContentJson: String,
    val plainTextCache: String,
    val entryTimestamp: Long,
    val updatedAt: Long,
    val latitude: Double?,
    val longitude: Double?,
    val locationName: String?,
    val cityName: String?,
    val areaName: String?,
    val streetName: String?,
    val isFavorite: Boolean,
    val isTrashed: Boolean,
    val trashedAt: Long?,
    val media: List<MediaAttachment>,
    val tags: List<Tag>,
    val people: List<Person>
) {
    val firstPhoto: MediaAttachment?
        get() = media.firstOrNull { it.type == MediaType.PHOTO }
}

data class BookStats(
    val bookTitle: String,
    val bookColorHex: String,
    val yearSpanLabel: String,
    val streak: Int,
    val entriesCount: Int,
    val daysCount: Int,
    val mediaCount: Int,
    val onThisDayCount: Int,
    val thisWeekCount: Int,
    val placesCount: Int,
    val tagsCount: Int,
    val videosCount: Int,
    val photosCount: Int,
    val audiosCount: Int,
    val filesCount: Int
)

data class PersonSummary(
    val person: Person,
    val entriesCount: Int,
    val lastInteractedAt: Long?
)

data class EntryTemplate(
    val id: String,
    val name: String,
    val description: String,
    val titleSuggestion: String,
    val document: RichDocument
)

object DefaultTemplates {
    val all: List<EntryTemplate> = listOf(
        EntryTemplate(
            id = "tpl_daily_checkin",
            name = "Daily Check-In",
            description = "Highlight, gratitude, and tomorrow's focus",
            titleSuggestion = "Daily Check-In",
            document = RichDocument(
                blocks = listOf(
                    RichBlock(type = BlockType.HEADING_2, text = "Today's Highlight"),
                    RichBlock(type = BlockType.NORMAL, text = ""),
                    RichBlock(type = BlockType.HEADING_2, text = "Gratitude"),
                    RichBlock(type = BlockType.BULLET_LIST, text = ""),
                    RichBlock(type = BlockType.HEADING_2, text = "Action Items"),
                    RichBlock(type = BlockType.CHECKLIST, text = "", checked = false)
                )
            )
        ),
        EntryTemplate(
            id = "tpl_gratitude_me",
            name = "Gratitude About Me",
            description = "Appreciate your own effort and growth",
            titleSuggestion = "Gratitude About Me",
            document = RichDocument(
                blocks = listOf(
                    RichBlock(type = BlockType.QUOTE, text = "What did I handle well today?"),
                    RichBlock(type = BlockType.NORMAL, text = ""),
                    RichBlock(type = BlockType.HEADING_2, text = "Three Things I Appreciate About Myself"),
                    RichBlock(type = BlockType.NUMBERED_LIST, text = ""),
                    RichBlock(type = BlockType.NUMBERED_LIST, text = ""),
                    RichBlock(type = BlockType.NUMBERED_LIST, text = "")
                )
            )
        ),
        EntryTemplate(
            id = "tpl_person_catchup",
            name = "People & Catch-Up",
            description = "Log conversations, trips, and moments with someone",
            titleSuggestion = "Catch-Up Notes",
            document = RichDocument(
                blocks = listOf(
                    RichBlock(type = BlockType.HEADING_2, text = "Where & Who"),
                    RichBlock(type = BlockType.NORMAL, text = ""),
                    RichBlock(type = BlockType.HEADING_2, text = "What We Talked About"),
                    RichBlock(type = BlockType.BULLET_LIST, text = ""),
                    RichBlock(type = BlockType.HEADING_2, text = "Remember For Next Time"),
                    RichBlock(type = BlockType.CHECKLIST, text = "")
                )
            )
        ),
        EntryTemplate(
            id = "tpl_fitness_log",
            name = "Fitness & Energy",
            description = "Track workout, movement, sleep, and mood",
            titleSuggestion = "Workout & Energy Log",
            document = RichDocument(
                blocks = listOf(
                    RichBlock(type = BlockType.HEADING_2, text = "Movement & Training"),
                    RichBlock(type = BlockType.NORMAL, text = ""),
                    RichBlock(type = BlockType.HEADING_2, text = "How My Body Feels"),
                    RichBlock(type = BlockType.NORMAL, text = "")
                )
            )
        )
    )
}
