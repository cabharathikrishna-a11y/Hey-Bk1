package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.domain.model.MediaType

@Entity(tableName = "journal_books")
data class JournalBookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val colorHex: String,
    val iconName: String,
    val createdAt: Long,
    val isDefault: Boolean = false
)

@Entity(
    tableName = "journal_entries",
    foreignKeys = [
        ForeignKey(
            entity = JournalBookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("bookId"),
        Index("entryTimestamp"),
        Index("isTrashed")
    ]
)
data class JournalEntryEntity(
    @PrimaryKey val id: String,
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
    val trashedAt: Long? = null
)

@Entity(
    tableName = "entry_versions",
    foreignKeys = [
        ForeignKey(
            entity = JournalEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("entryId"), Index("savedAt")]
)
data class EntryVersionEntity(
    @PrimaryKey val id: String,
    val entryId: String,
    val titleSnapshot: String,
    val richContentJson: String,
    val savedAt: Long,
    val summaryLabel: String
)

@Entity(
    tableName = "media_attachments",
    foreignKeys = [
        ForeignKey(
            entity = JournalEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("entryId"), Index("type")]
)
data class MediaAttachmentEntity(
    @PrimaryKey val id: String,
    val entryId: String,
    val type: String, // MediaType.name
    val localUri: String,
    val displayName: String,
    val durationMs: Long = 0L,
    val createdAt: Long
) {
    fun mediaType(): MediaType = runCatching { MediaType.valueOf(type) }.getOrDefault(MediaType.DOCUMENT)
}

@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)]
)
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String
)

@Entity(
    tableName = "entry_tag_cross_ref",
    primaryKeys = ["entryId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = JournalEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tagId")]
)
data class EntryTagCrossRef(
    val entryId: String,
    val tagId: String
)

@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val contactLookupUri: String? = null,
    val note: String = ""
)

@Entity(
    tableName = "entry_person_cross_ref",
    primaryKeys = ["entryId", "personId"],
    foreignKeys = [
        ForeignKey(
            entity = JournalEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("personId")]
)
data class EntryPersonCrossRef(
    val entryId: String,
    val personId: String
)

@Entity(tableName = "journal_prompts")
data class JournalPromptEntity(
    @PrimaryKey val id: String,
    val category: String,
    val promptText: String,
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false
)

data class JournalEntryWithRelations(
    @Embedded val entry: JournalEntryEntity,
    @Relation(
        parentColumn = "bookId",
        entityColumn = "id"
    )
    val book: JournalBookEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "entryId"
    )
    val media: List<MediaAttachmentEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = EntryTagCrossRef::class,
            parentColumn = "entryId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = EntryPersonCrossRef::class,
            parentColumn = "entryId",
            entityColumn = "personId"
        )
    )
    val people: List<PersonEntity>
)
