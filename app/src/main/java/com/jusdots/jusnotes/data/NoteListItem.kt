package com.jusdots.jusnotes.data

import androidx.compose.runtime.Immutable

/**
 * Lightweight projection for the note list (Phase 3).
 * Carries only what the card needs; full Note (with content/checklist/images/mapItems)
 * stays available via getNoteById for editor/viewer.
 *
 * Room populates this via the projection query in NoteDao.
 * `preview` is substr(content,1,200) so large bodies don't transit DB→heap for the list.
 */
@Immutable
data class NoteListItem(
    val id: Int,
    val title: String,
    val preview: String,
    val timestamp: Long,
    val lastModified: Long,
    val isPinned: Boolean,
    val isChecklist: Boolean,
    val isMap: Boolean,
    val isSecret: Boolean,
    val color: Int?,
    val tags: List<String> = emptyList(),
    val linkedNoteIds: List<Int> = emptyList(),
    // lightweight counts so checklist cards can render without full list
    val checklist: List<ChecklistItem> = emptyList(),
)
