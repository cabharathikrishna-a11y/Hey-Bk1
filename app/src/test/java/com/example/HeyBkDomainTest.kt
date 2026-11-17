package com.example

import com.example.data.repository.computeBookStats
import com.example.domain.model.BlockType
import com.example.domain.model.InlineMarkType
import com.example.domain.model.JournalEntryDetail
import com.example.domain.model.MediaAttachment
import com.example.domain.model.MediaType
import com.example.domain.model.RichBlock
import com.example.domain.model.RichDocument
import com.example.domain.model.Tag
import com.example.ui.state.EditorDraftState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class HeyBkDomainTest {

    @Test
    fun richDocument_roundTripAndMarkdownExport() {
        val doc = RichDocument(
            blocks = listOf(
                RichBlock(id = "1", type = BlockType.HEADING_1, text = "Morning Walk"),
                RichBlock(id = "2", type = BlockType.CHECKLIST, text = "Drink water", checked = true),
                RichBlock(id = "3", type = BlockType.QUOTE, text = "Stay present")
            )
        )
        val encoded = RichDocument.encode(doc)
        val decoded = RichDocument.decode(encoded)
        assertEquals(3, decoded.blocks.size)
        assertEquals(BlockType.HEADING_1, decoded.blocks[0].type)
        assertTrue(decoded.blocks[1].checked)
        val md = decoded.toMarkdown()
        assertTrue(md.contains("# Morning Walk"))
        assertTrue(md.contains("- [x] Drink water"))
        assertTrue(md.contains("> Stay present"))
    }

    @Test
    fun editorDraft_undoRedoAndFormatting() {
        val initial = EditorDraftState(
            title = "First",
            blocks = listOf(RichBlock(id = "b1", text = "Hello world"))
        )
        val formatted = initial
            .withBlockType(0, BlockType.HEADING_2)
            .withInlineMarkToggled(0, 0, 5, InlineMarkType.BOLD)

        assertEquals(BlockType.HEADING_2, formatted.blocks[0].type)
        assertEquals(1, formatted.blocks[0].spans.size)
        assertTrue(formatted.canUndo)

        val undone = formatted.undo()
        assertTrue(undone.blocks[0].spans.isEmpty())
        assertTrue(undone.canRedo)

        val redone = undone.redo()
        assertEquals(1, redone.blocks[0].spans.size)
        assertFalse(redone.canRedo)
    }

    @Test
    fun bookStats_computesStreakOnThisDayAndMediaBreakdown() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 17, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val yesterdayCal = (nowCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val lastYearCal = (nowCal.clone() as Calendar).apply { add(Calendar.YEAR, -1) }

        val entries = listOf(
            sampleEntry("e1", nowCal.timeInMillis, MediaType.PHOTO, "Guntur"),
            sampleEntry("e2", yesterdayCal.timeInMillis, MediaType.VIDEO, "Guntur"),
            sampleEntry("e3", lastYearCal.timeInMillis, MediaType.AUDIO, "Vijayawada")
        )

        val stats = computeBookStats("All Entries", "#78C6FF", entries, nowCal.timeInMillis)
        assertEquals(2, stats.streak)
        assertEquals(3, stats.entriesCount)
        assertEquals(3, stats.daysCount)
        assertEquals(1, stats.onThisDayCount)
        assertEquals(2, stats.placesCount)
        assertEquals(1, stats.photosCount)
        assertEquals(1, stats.videosCount)
        assertEquals(1, stats.audiosCount)
        assertEquals(3, stats.mediaCount)
    }

    private fun sampleEntry(
        id: String,
        ts: Long,
        mediaType: MediaType,
        city: String
    ) = JournalEntryDetail(
        id = id,
        bookId = "book_diary",
        bookTitle = "Diary",
        bookColorHex = "#F5B700",
        title = "Entry $id",
        richContentJson = "",
        plainTextCache = "Body $id",
        entryTimestamp = ts,
        updatedAt = ts,
        latitude = 16.3,
        longitude = 80.4,
        locationName = city,
        cityName = city,
        areaName = null,
        streetName = null,
        isFavorite = false,
        isTrashed = false,
        trashedAt = null,
        media = listOf(MediaAttachment("m_$id", id, mediaType, "file:///tmp/x", "x", 0L, ts)),
        tags = listOf(Tag("t1", "life")),
        people = emptyList()
    )
}
