package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.domain.model.PromptCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        JournalBookEntity::class,
        JournalEntryEntity::class,
        EntryVersionEntity::class,
        MediaAttachmentEntity::class,
        TagEntity::class,
        EntryTagCrossRef::class,
        PersonEntity::class,
        EntryPersonCrossRef::class,
        JournalPromptEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HeyBkDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao

    companion object {
        @Volatile
        private var INSTANCE: HeyBkDatabase? = null

        fun getInstance(context: Context): HeyBkDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HeyBkDatabase::class.java,
                    "hey_bk_journal.db"
                )
                    .addCallback(SeedDatabaseCallback(context.applicationContext))
                    .build()
                    .also { INSTANCE = it }
            }
        }

        val defaultBooks = listOf(
            JournalBookEntity(
                id = "book_diary",
                title = "Diary",
                colorHex = "#F5B700",
                iconName = "book",
                createdAt = 1L,
                isDefault = true
            ),
            JournalBookEntity(
                id = "book_journal",
                title = "Journal",
                colorHex = "#4AA8E8",
                iconName = "journal",
                createdAt = 2L,
                isDefault = false
            ),
            JournalBookEntity(
                id = "book_bookmarks",
                title = "Bookmarks",
                colorHex = "#1E6B7B",
                iconName = "bookmark",
                createdAt = 3L,
                isDefault = false
            )
        )

        val defaultPrompts = listOf(
            // Reflection
            JournalPromptEntity("prm_ref_1", PromptCategory.REFLECTION.title, "What moment today changed how you saw things—even slightly?"),
            JournalPromptEntity("prm_ref_2", PromptCategory.REFLECTION.title, "What did you avoid today, and what was underneath that hesitation?"),
            JournalPromptEntity("prm_ref_3", PromptCategory.REFLECTION.title, "If today had a single honest headline, what would it be?"),
            // Gratitude about me
            JournalPromptEntity("prm_gam_1", PromptCategory.GRATITUDE_ABOUT_ME.title, "What is one quiet strength you showed yourself this week?"),
            JournalPromptEntity("prm_gam_2", PromptCategory.GRATITUDE_ABOUT_ME.title, "Thank yourself for one hard boundary or patient decision you made recently."),
            JournalPromptEntity("prm_gam_3", PromptCategory.GRATITUDE_ABOUT_ME.title, "What part of who you are becoming makes you genuinely proud?"),
            // Mindfulness
            JournalPromptEntity("prm_mnd_1", PromptCategory.MINDFULNESS.title, "Notice five sensory details around you right now—sounds, light, textures."),
            JournalPromptEntity("prm_mnd_2", PromptCategory.MINDFULNESS.title, "Where are you holding tension in your body, and what needs space?"),
            JournalPromptEntity("prm_mnd_3", PromptCategory.MINDFULNESS.title, "Describe the quietest five minutes of your day."),
            // Fun
            JournalPromptEntity("prm_fun_1", PromptCategory.FUN.title, "What made you laugh without even expecting it lately?"),
            JournalPromptEntity("prm_fun_2", PromptCategory.FUN.title, "If you had a completely unplanned free afternoon tomorrow, what sounds genuinely fun?"),
            JournalPromptEntity("prm_fun_3", PromptCategory.FUN.title, "Log a small inside joke, meme, or funny exchange worth remembering."),
            // Fitness
            JournalPromptEntity("prm_fit_1", PromptCategory.FITNESS.title, "How did your body feel when you moved today? Energy before vs after?"),
            JournalPromptEntity("prm_fit_2", PromptCategory.FITNESS.title, "What small physical habit helped your focus or sleep this week?"),
            JournalPromptEntity("prm_fit_3", PromptCategory.FITNESS.title, "Record today's workout, walk, or recovery check-in."),
            // Creativity
            JournalPromptEntity("prm_cre_1", PromptCategory.CREATIVITY.title, "What raw idea, feature, or sketch popped into your head today?"),
            JournalPromptEntity("prm_cre_2", PromptCategory.CREATIVITY.title, "If you could build or design something purely for delight, what would it do?"),
            JournalPromptEntity("prm_cre_3", PromptCategory.CREATIVITY.title, "Capture an unfinished thought without judging whether it is good yet."),
            // Love
            JournalPromptEntity("prm_lov_1", PromptCategory.LOVE.title, "Who showed up for you recently, and how did it feel?"),
            JournalPromptEntity("prm_lov_2", PromptCategory.LOVE.title, "Write about a conversation with someone you care about that you don't want to forget."),
            JournalPromptEntity("prm_lov_3", PromptCategory.LOVE.title, "How did you express care—to someone else or to yourself—today?"),
            // Legacy
            JournalPromptEntity("prm_leg_1", PromptCategory.LEGACY.title, "What lesson from this chapter would you want your future self to read?"),
            JournalPromptEntity("prm_leg_2", PromptCategory.LEGACY.title, "What value are you practicing right now that will matter five years from today?"),
            JournalPromptEntity("prm_leg_3", PromptCategory.LEGACY.title, "Write a short note to yourself ten years from now."),
            // The Year Ahead
            JournalPromptEntity("prm_yra_1", PromptCategory.THE_YEAR_AHEAD.title, "What is one thing you want to build or complete over the next twelve months?"),
            JournalPromptEntity("prm_yra_2", PromptCategory.THE_YEAR_AHEAD.title, "What habit or clutter are you ready to leave behind before next year?"),
            JournalPromptEntity("prm_yra_3", PromptCategory.THE_YEAR_AHEAD.title, "What kind of memories do you want filling your calendar this year?")
        )
    }

    private class SeedDatabaseCallback(private val context: Context) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                val dao = getInstance(context).journalDao()
                dao.insertBooks(defaultBooks)
                dao.insertPrompts(defaultPrompts)
            }
        }
    }
}
