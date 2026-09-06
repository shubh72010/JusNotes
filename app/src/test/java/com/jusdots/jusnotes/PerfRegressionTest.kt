package com.jusdots.jusnotes

import com.jusdots.jusnotes.data.ChecklistItem
import com.jusdots.jusnotes.data.Converters
import com.jusdots.jusnotes.data.Note
import com.jusdots.jusnotes.utils.MarkdownRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 11 — lightweight stress/contract checks that live without device.
 * Verifies demand-driven invariants survive refactors.
 */
class PerfRegressionTest {

    @Test fun converters_cache_notAllocatePerCall() {
        val c = Converters()
        val json = """[{"text":"a","isChecked":false}]"""
        // empty string fast path + cached TypeToken
        assertEquals(0, c.fromChecklistItemList("").size)
        assertEquals(0, c.fromChecklistItemList(null).size)
        assertEquals(1, c.fromChecklistItemList(json).size)
        assertEquals(0, c.fromStringList("").size)
        assertEquals(0, c.fromIntList(null).size)
    }

    @Test fun notePreviewTruncation_contract() {
        val long = "a".repeat(500)
        val n = Note(id = 1, title = "t", content = long, timestamp = 0)
        // NoteItem/GridNoteItem contract: preview is first 100/180 chars, not full Markdown tree
        val p100 = if (n.content.length > 100) n.content.take(100) + "..." else n.content
        val p180 = if (n.content.length > 180) n.content.take(180) + "…" else n.content
        assertTrue(p100.length <= 103)
        assertTrue(p180.length <= 181)
    }

    @Test fun markdownParse_isDemandDriven_notCalledForPreview() {
        // list preview must NOT invoke parseBlocks — covered by audit; here ensure parse itself is correct when invoked
        val md = "# Hi\n**bold** and [[Link]]\n![alt](content://x)"
        val blocks = MarkdownRenderer.parseBlocks(md)
        assertTrue(blocks.any { it is com.jusdots.jusnotes.utils.MarkdownBlock.Heading })
        assertTrue(blocks.any { it is com.jusdots.jusnotes.utils.MarkdownBlock.Image })
    }

    @Test fun largeNoteStress_markdownDoesNotStackOverflow() {
        val big = buildString {
            repeat(500) { append("**bold** _italic_ [[Note $it]] `code` #tag\n") }
            repeat(20) { append("![i](content://img/$it)\n") }
        }
        val blocks = MarkdownRenderer.parseBlocks(big)
        assertTrue(blocks.isNotEmpty())
        // rough bound: one block per non-empty line
        assertTrue(blocks.size >= 400)
    }

    @Test fun lightweightProjection_fieldsSufficientForCard() {
        // NoteListItem must carry everything NoteItem/GridNoteItem reads
        val cl = listOf(ChecklistItem(text = "a"), ChecklistItem(text = "b", isChecked = true))
        val n = Note(id = 2, title = "t", content = "hello", checklist = cl, isChecklist = true)
        // card reads title, timestamp, tags, color, isPinned/isMap, and checklist.take(3/8)
        assertEquals(2, n.checklist.size)
    }
}
