package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {

    @Query("SELECT * FROM journal_books ORDER BY isDefault DESC, createdAt ASC")
    fun observeBooks(): Flow<List<JournalBookEntity>>

    @Query("SELECT * FROM journal_books ORDER BY isDefault DESC, createdAt ASC")
    suspend fun getAllBooksOnce(): List<JournalBookEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: JournalBookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<JournalBookEntity>)

    @Query("DELETE FROM journal_books WHERE id = :bookId AND isDefault = 0")
    suspend fun deleteBook(bookId: String)

    @Transaction
    @Query("SELECT * FROM journal_entries WHERE isTrashed = 0 ORDER BY entryTimestamp DESC")
    fun observeActiveEntries(): Flow<List<JournalEntryWithRelations>>

    @Transaction
    @Query("SELECT * FROM journal_entries WHERE isTrashed = 1 ORDER BY trashedAt DESC")
    fun observeTrashedEntries(): Flow<List<JournalEntryWithRelations>>

    @Transaction
    @Query("SELECT * FROM journal_entries WHERE id = :entryId")
    fun observeEntryById(entryId: String): Flow<JournalEntryWithRelations?>

    @Transaction
    @Query("SELECT * FROM journal_entries WHERE id = :entryId")
    suspend fun getEntryByIdOnce(entryId: String): JournalEntryWithRelations?

    @Transaction
    @Query("SELECT * FROM journal_entries ORDER BY entryTimestamp DESC")
    suspend fun getAllEntriesOnce(): List<JournalEntryWithRelations>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntryEntity)

    @Update
    suspend fun updateEntry(entry: JournalEntryEntity)

    @Query("UPDATE journal_entries SET isFavorite = :favorite WHERE id = :entryId")
    suspend fun setEntryFavorite(entryId: String, favorite: Boolean)

    @Query("UPDATE journal_entries SET bookId = :targetBookId, updatedAt = :updatedAt WHERE id = :entryId")
    suspend fun moveEntryToBook(entryId: String, targetBookId: String, updatedAt: Long)

    @Query("UPDATE journal_entries SET isTrashed = 1, trashedAt = :trashedAt WHERE id = :entryId")
    suspend fun moveToTrash(entryId: String, trashedAt: Long)

    @Query("UPDATE journal_entries SET isTrashed = 0, trashedAt = NULL WHERE id = :entryId")
    suspend fun restoreFromTrash(entryId: String)

    @Query("DELETE FROM journal_entries WHERE id = :entryId")
    suspend fun deleteEntryPermanently(entryId: String)

    @Query("DELETE FROM journal_entries WHERE isTrashed = 1")
    suspend fun emptyTrash()

    // Media
    @Query("SELECT * FROM media_attachments WHERE entryId = :entryId ORDER BY createdAt ASC")
    suspend fun getMediaForEntry(entryId: String): List<MediaAttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: List<MediaAttachmentEntity>)

    @Query("DELETE FROM media_attachments WHERE entryId = :entryId")
    suspend fun deleteMediaForEntry(entryId: String)

    // Versions
    @Query("SELECT * FROM entry_versions WHERE entryId = :entryId ORDER BY savedAt DESC")
    fun observeVersionsForEntry(entryId: String): Flow<List<EntryVersionEntity>>

    @Query("SELECT * FROM entry_versions")
    suspend fun getAllVersionsOnce(): List<EntryVersionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: EntryVersionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersions(versions: List<EntryVersionEntity>)

    // Tags
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun getAllTagsOnce(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findTagByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<TagEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntryTagRefs(refs: List<EntryTagCrossRef>)

    @Query("DELETE FROM entry_tag_cross_ref WHERE entryId = :entryId")
    suspend fun clearTagsForEntry(entryId: String)

    // People
    @Query("SELECT * FROM people ORDER BY displayName ASC")
    fun observeAllPeople(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people ORDER BY displayName ASC")
    suspend fun getAllPeopleOnce(): List<PersonEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: PersonEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeople(people: List<PersonEntity>)

    @Query("DELETE FROM people WHERE id = :personId")
    suspend fun deletePerson(personId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntryPersonRefs(refs: List<EntryPersonCrossRef>)

    @Query("DELETE FROM entry_person_cross_ref WHERE entryId = :entryId")
    suspend fun clearPeopleForEntry(entryId: String)

    // Prompts
    @Query("SELECT * FROM journal_prompts ORDER BY isCustom DESC, category ASC")
    fun observeAllPrompts(): Flow<List<JournalPromptEntity>>

    @Query("SELECT * FROM journal_prompts")
    suspend fun getAllPromptsOnce(): List<JournalPromptEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrompt(prompt: JournalPromptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrompts(prompts: List<JournalPromptEntity>)

    @Query("UPDATE journal_prompts SET isFavorite = :favorite WHERE id = :promptId")
    suspend fun setPromptFavorite(promptId: String, favorite: Boolean)

    @Query("DELETE FROM journal_prompts WHERE id = :promptId AND isCustom = 1")
    suspend fun deleteCustomPrompt(promptId: String)

    @Transaction
    suspend fun saveFullEntry(
        entry: JournalEntryEntity,
        media: List<MediaAttachmentEntity>,
        tagIds: List<String>,
        personIds: List<String>,
        version: EntryVersionEntity?
    ) {
        insertEntry(entry)
        deleteMediaForEntry(entry.id)
        if (media.isNotEmpty()) {
            insertMedia(media)
        }
        clearTagsForEntry(entry.id)
        if (tagIds.isNotEmpty()) {
            insertEntryTagRefs(tagIds.map { EntryTagCrossRef(entry.id, it) })
        }
        clearPeopleForEntry(entry.id)
        if (personIds.isNotEmpty()) {
            insertEntryPersonRefs(personIds.map { EntryPersonCrossRef(entry.id, it) })
        }
        if (version != null) {
            insertVersion(version)
        }
    }

    @Transaction
    suspend fun importFullSnapshot(
        books: List<JournalBookEntity>,
        tags: List<TagEntity>,
        people: List<PersonEntity>,
        prompts: List<JournalPromptEntity>,
        entries: List<JournalEntryEntity>,
        media: List<MediaAttachmentEntity>,
        tagRefs: List<EntryTagCrossRef>,
        personRefs: List<EntryPersonCrossRef>,
        versions: List<EntryVersionEntity>
    ) {
        if (books.isNotEmpty()) insertBooks(books)
        if (tags.isNotEmpty()) insertTags(tags)
        if (people.isNotEmpty()) insertPeople(people)
        if (prompts.isNotEmpty()) insertPrompts(prompts)
        entries.forEach { insertEntry(it) }
        if (media.isNotEmpty()) insertMedia(media)
        if (tagRefs.isNotEmpty()) insertEntryTagRefs(tagRefs)
        if (personRefs.isNotEmpty()) insertEntryPersonRefs(personRefs)
        if (versions.isNotEmpty()) insertVersions(versions)
    }
}
