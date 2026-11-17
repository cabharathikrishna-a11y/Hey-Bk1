package com.example.ui.state

import com.example.domain.model.BlockType
import com.example.domain.model.EntryTemplate
import com.example.domain.model.InlineMarkType
import com.example.domain.model.InlineSpan
import com.example.domain.model.JournalEntryDetail
import com.example.domain.model.MediaAttachment
import com.example.domain.model.Person
import com.example.domain.model.RichBlock
import com.example.domain.model.RichDocument
import java.util.UUID

data class EditorSnapshot(
    val title: String,
    val blocks: List<RichBlock>
)

data class EditorDraftState(
    val entryId: String? = null,
    val bookId: String = "book_diary",
    val title: String = "",
    val blocks: List<RichBlock> = listOf(RichBlock()),
    val focusedBlockIndex: Int = 0,
    val selectionStart: Int = 0,
    val selectionEnd: Int = 0,
    val entryTimestamp: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
    val cityName: String? = null,
    val areaName: String? = null,
    val streetName: String? = null,
    val isFavorite: Boolean = false,
    val media: List<MediaAttachment> = emptyList(),
    val tagNames: List<String> = emptyList(),
    val people: List<Person> = emptyList(),
    val undoStack: List<EditorSnapshot> = emptyList(),
    val redoStack: List<EditorSnapshot> = emptyList(),
    val inEntrySearchVisible: Boolean = false,
    val inEntrySearchQuery: String = "",
    val isCapturingLocation: Boolean = false
) {
    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun toDocument(): RichDocument = RichDocument(blocks = blocks)

    fun pushUndo(): EditorDraftState {
        val snapshot = EditorSnapshot(title = title, blocks = blocks)
        val trimmed = (undoStack + snapshot).takeLast(40)
        return copy(undoStack = trimmed, redoStack = emptyList())
    }

    fun undo(): EditorDraftState {
        if (undoStack.isEmpty()) return this
        val last = undoStack.last()
        val current = EditorSnapshot(title = title, blocks = blocks)
        return copy(
            title = last.title,
            blocks = last.blocks,
            undoStack = undoStack.dropLast(1),
            redoStack = (redoStack + current).takeLast(40),
            focusedBlockIndex = focusedBlockIndex.coerceIn(0, (last.blocks.size - 1).coerceAtLeast(0))
        )
    }

    fun redo(): EditorDraftState {
        if (redoStack.isEmpty()) return this
        val next = redoStack.last()
        val current = EditorSnapshot(title = title, blocks = blocks)
        return copy(
            title = next.title,
            blocks = next.blocks,
            undoStack = (undoStack + current).takeLast(40),
            redoStack = redoStack.dropLast(1),
            focusedBlockIndex = focusedBlockIndex.coerceIn(0, (next.blocks.size - 1).coerceAtLeast(0))
        )
    }

    fun withUpdatedBlockText(index: Int, newText: String): EditorDraftState {
        if (index !in blocks.indices) return this
        val oldBlock = blocks[index]
        if (oldBlock.text == newText) return this

        // Handle newline inside block -> split into a new block seamlessly
        if (newText.contains('\n')) {
            val parts = newText.split('\n')
            val firstText = parts.first()
            val nextType = when (oldBlock.type) {
                BlockType.BULLET_LIST, BlockType.NUMBERED_LIST, BlockType.CHECKLIST -> oldBlock.type
                else -> BlockType.NORMAL
            }
            val updatedFirst = oldBlock.copy(
                text = firstText,
                spans = oldBlock.spans.mapNotNull { span ->
                    val s = span.start.coerceAtMost(firstText.length)
                    val e = span.end.coerceAtMost(firstText.length)
                    if (s < e) span.copy(start = s, end = e) else null
                }
            )
            val newBlocks = parts.drop(1).map { line ->
                RichBlock(id = UUID.randomUUID().toString(), type = nextType, text = line)
            }
            val resultList = blocks.toMutableList().apply {
                this[index] = updatedFirst
                addAll(index + 1, newBlocks)
            }
            return pushUndo().copy(
                blocks = resultList,
                focusedBlockIndex = (index + newBlocks.size).coerceIn(0, resultList.lastIndex),
                selectionStart = 0,
                selectionEnd = 0
            )
        }

        val adjustedSpans = oldBlock.spans.mapNotNull { span ->
            val s = span.start.coerceIn(0, newText.length)
            val e = span.end.coerceIn(0, newText.length)
            if (s < e) span.copy(start = s, end = e) else null
        }
        val next = blocks.toMutableList().apply {
            this[index] = oldBlock.copy(text = newText, spans = adjustedSpans)
        }
        return pushUndo().copy(blocks = next)
    }

    fun withBlockType(index: Int, type: BlockType): EditorDraftState {
        if (index !in blocks.indices) return this
        val target = blocks[index]
        val nextType = if (target.type == type && type != BlockType.NORMAL) BlockType.NORMAL else type
        val next = blocks.toMutableList().apply {
            this[index] = target.copy(type = nextType)
        }
        return pushUndo().copy(blocks = next)
    }

    fun withChecklistToggled(index: Int): EditorDraftState {
        if (index !in blocks.indices) return this
        val target = blocks[index]
        val next = blocks.toMutableList().apply {
            this[index] = target.copy(checked = !target.checked)
        }
        return pushUndo().copy(blocks = next)
    }

    fun withMergedOrDeletedEmptyBlock(index: Int): EditorDraftState {
        if (index <= 0 || index !in blocks.indices) return this
        val current = blocks[index]
        if (current.type != BlockType.NORMAL) {
            return withBlockType(index, BlockType.NORMAL)
        }
        if (current.text.isEmpty() && blocks.size > 1) {
            val next = blocks.toMutableList().apply { removeAt(index) }
            val prevIdx = (index - 1).coerceAtLeast(0)
            val prevLen = next[prevIdx].text.length
            return pushUndo().copy(
                blocks = next,
                focusedBlockIndex = prevIdx,
                selectionStart = prevLen,
                selectionEnd = prevLen
            )
        }
        return this
    }

    fun withInlineMarkToggled(
        blockIndex: Int,
        start: Int,
        end: Int,
        mark: InlineMarkType,
        url: String? = null
    ): EditorDraftState {
        if (blockIndex !in blocks.indices) return this
        val block = blocks[blockIndex]
        if (block.text.isEmpty()) return this
        val actualStart: Int
        val actualEnd: Int
        if (start != end) {
            actualStart = minOf(start, end).coerceIn(0, block.text.length)
            actualEnd = maxOf(start, end).coerceIn(0, block.text.length)
        } else {
            // Apply to whole block text if no range was highlighted
            actualStart = 0
            actualEnd = block.text.length
        }
        if (actualStart >= actualEnd) return this

        val existing = block.spans.firstOrNull {
            it.type == mark && it.start == actualStart && it.end == actualEnd
        }
        val updatedSpans = if (existing != null && mark != InlineMarkType.LINK) {
            block.spans.filterNot { it == existing }
        } else {
            val filtered = if (mark == InlineMarkType.LINK) {
                block.spans.filterNot { it.type == InlineMarkType.LINK && it.start == actualStart && it.end == actualEnd }
            } else block.spans
            filtered + InlineSpan(actualStart, actualEnd, mark, url)
        }

        val next = blocks.toMutableList().apply {
            this[blockIndex] = block.copy(spans = updatedSpans)
        }
        return pushUndo().copy(blocks = next)
    }

    fun withTemplateApplied(template: EntryTemplate): EditorDraftState {
        val nextBlocks = if (blocks.size == 1 && blocks.first().text.isBlank()) {
            template.document.blocks.map { it.copy(id = UUID.randomUUID().toString()) }
        } else {
            blocks + template.document.blocks.map { it.copy(id = UUID.randomUUID().toString()) }
        }
        return pushUndo().copy(
            title = title.ifBlank { template.titleSuggestion },
            blocks = nextBlocks
        )
    }

    fun withAppendedDictation(spoken: String): EditorDraftState {
        val clean = spoken.trim()
        if (clean.isEmpty()) return this
        val idx = focusedBlockIndex.coerceIn(0, blocks.lastIndex)
        val current = blocks[idx]
        val joined = if (current.text.isBlank()) clean else "${current.text.trimEnd()} $clean"
        return withUpdatedBlockText(idx, joined)
    }

    companion object {
        fun fromEntry(entry: JournalEntryDetail): EditorDraftState {
            val doc = RichDocument.decode(entry.richContentJson)
            return EditorDraftState(
                entryId = entry.id,
                bookId = entry.bookId,
                title = entry.title,
                blocks = doc.blocks.ifEmpty { listOf(RichBlock()) },
                entryTimestamp = entry.entryTimestamp,
                updatedAt = entry.updatedAt,
                latitude = entry.latitude,
                longitude = entry.longitude,
                locationName = entry.locationName,
                cityName = entry.cityName,
                areaName = entry.areaName,
                streetName = entry.streetName,
                isFavorite = entry.isFavorite,
                media = entry.media,
                tagNames = entry.tags.map { it.name },
                people = entry.people
            )
        }
    }
}
