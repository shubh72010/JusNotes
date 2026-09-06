package com.jusdots.jusnotes.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jusdots.jusnotes.data.AppDatabase
import com.jusdots.jusnotes.data.Note
import com.jusdots.jusnotes.data.NoteRepository
import com.jusdots.jusnotes.data.SettingsManager
import com.jusdots.jusnotes.network.ChatRequest
import com.jusdots.jusnotes.network.Message
import com.jusdots.jusnotes.network.NetworkModule
import com.jusdots.jusnotes.utils.BackupUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class NoteTab {
    NOTES, CHECKLISTS, MAPS, SEARCH
}

enum class SortOrder(val label: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    TITLE_ASC("Title A→Z"),
    TITLE_DESC("Title Z→A"),
    MODIFIED("Last Modified")
}

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = NoteRepository(db.noteDao())
    private val settingsManager = SettingsManager(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Debounced search query for performance
    private val debouncedSearchQuery = _searchQuery
        .debounce(300L)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _selectedMapId = MutableStateFlow<Int?>(null)
    val selectedMapId: StateFlow<Int?> = _selectedMapId

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _selectedTab = MutableStateFlow(NoteTab.NOTES)
    val selectedTab: StateFlow<NoteTab> = _selectedTab

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag

    private val _isSecretAuthenticated = MutableStateFlow(false)
    val isSecretAuthenticated: StateFlow<Boolean> = _isSecretAuthenticated

    val viewMode: StateFlow<String> = settingsManager.viewMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "list")

    val apiKey = settingsManager.apiKey.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val modelId = settingsManager.modelId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "google/gemini-flash-1.5")
    val theme = settingsManager.themePref.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Dark")
    val isAnimationsEnabled = settingsManager.isAnimationsEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val storageUri = settingsManager.storageUri.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val hasShownGestureHints = settingsManager.hasShownGestureHints.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    val allNonDeletedNotes: StateFlow<List<Note>> = repository.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ponytail: derive from single source — old code ran 3 identical SELECT * queries in parallel
    val mapNotes: StateFlow<List<Note>> = allNonDeletedNotes
        .map { notes -> notes.filter { it.isMap } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val filterCombiner = combine(
        debouncedSearchQuery,
        _selectedMapId,
        _selectedTab
    ) { query, mapId, tab ->
        Triple(query, mapId, tab)
    }

    private val sortCombiner = combine(
        _sortOrder,
        _selectedTag
    ) { sort, tag ->
        sort to tag
    }

    val notes: StateFlow<List<Note>> = combine(
        allNonDeletedNotes,
        filterCombiner,
        sortCombiner
    ) { notes, filters, sorting ->
        val (query, mapId, tab) = filters
        val (sort, tag) = sorting

        var filteredNotes = notes

        // Filter by Tab — homescreen (NOTES) shows all notes + checklists; MAPS separate
        filteredNotes = filteredNotes.filter { note ->
            when (tab) {
                NoteTab.NOTES -> !note.isMap
                NoteTab.CHECKLISTS -> note.isChecklist && !note.isMap
                NoteTab.MAPS -> note.isMap
                NoteTab.SEARCH -> true
            }
        }

        // Filter by search query
        if (query.isNotBlank()) {
            filteredNotes = filteredNotes.filter { note ->
                note.title.contains(query, ignoreCase = true) || 
                note.content.contains(query, ignoreCase = true) ||
                note.tags.any { t -> t.contains(query, ignoreCase = true) }
            }
        }

        // Filter by mapId (Replacement for Folders)
        if (mapId != null) {
            val selectedMapNote = notes.find { it.id == mapId }
            if (selectedMapNote != null) {
                filteredNotes = filteredNotes.filter { note -> 
                    note.id in selectedMapNote.linkedNoteIds || note.id == mapId
                }
            }
        }

        // Filter by tag
        if (tag != null) {
            filteredNotes = filteredNotes.filter { note -> tag in note.tags }
        }

        // Exclude secret notes
        filteredNotes = filteredNotes.filter { note -> !note.isSecret }

        // Apply sort — pinned always on top (ponytail: stable partition, no extra query)
        val baseSorted = when (sort) {
            SortOrder.DATE_DESC -> filteredNotes.sortedByDescending { it.timestamp }
            SortOrder.DATE_ASC -> filteredNotes.sortedBy { it.timestamp }
            SortOrder.TITLE_ASC -> filteredNotes.sortedBy { it.title.lowercase() }
            SortOrder.TITLE_DESC -> filteredNotes.sortedByDescending { it.title.lowercase() }
            SortOrder.MODIFIED -> filteredNotes.sortedByDescending { it.lastModified }
        }
        filteredNotes = baseSorted.sortedWith(compareByDescending<Note> { it.isPinned })

        filteredNotes
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val secretNotes: StateFlow<List<Note>> = combine(
        allNonDeletedNotes,
        debouncedSearchQuery
    ) { notes, query ->
        var filteredNotes = notes.filter { it.isSecret }
        if (query.isNotBlank()) {
            filteredNotes = filteredNotes.filter { 
                it.title.contains(query, ignoreCase = true) || 
                it.content.contains(query, ignoreCase = true) 
            }
        }
        filteredNotes
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pinnedNotes: StateFlow<List<Note>> = allNonDeletedNotes
        .map { notes -> notes.filter { it.isPinned } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedNotes: StateFlow<List<Note>> = repository.getDeletedNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All unique tags across all notes — derived from single source, flows on Default to avoid main thread work
    val allTags: StateFlow<List<String>> = allNonDeletedNotes
        .map { notes -> notes.flatMap { it.tags }.distinct().sorted() }
        .flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Lightweight preview list (Phase 3) — available for future list UI; keeps full Note path intact for now
    // Filtering still uses allNonDeletedNotes to preserve search correctness on full content.
    val noteListItems: StateFlow<List<com.jusdots.jusnotes.data.NoteListItem>> =
        repository.getNoteListItems().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Auto-cleanup policy: permanently delete trash typically older than 30 days
    init {
        viewModelScope.launch {
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            repository.permanentlyDeleteOldTrash(thirtyDaysAgo)
        }
    }

    fun setLoaded() {
        _isLoading.value = false
    }

    fun setSelectedMap(id: Int?) {
        _selectedMapId.value = id
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun setSelectedTag(tag: String?) {
        _selectedTag.value = tag
    }

    fun setSecretAuthenticated(authenticated: Boolean) {
        _isSecretAuthenticated.value = authenticated
    }

    // Folders removed, Map replacement logic in place.

    fun saveSettings(key: String, model: String, theme: String, animationsEnabled: Boolean) {
        viewModelScope.launch { settingsManager.saveSettings(key, model, theme, animationsEnabled) }
    }

    fun setStorageUri(uri: String?) {
        viewModelScope.launch { settingsManager.saveStorageUri(uri) }
    }

    fun setHasShownGestureHints(shown: Boolean) {
        viewModelScope.launch { settingsManager.saveHasShownGestureHints(shown) }
    }

    fun setViewMode(mode: String) {
        viewModelScope.launch { settingsManager.saveViewMode(mode) }
    }

    fun backupNotes(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val uri = storageUri.value
            if (uri == null) {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onError("Please set a backup directory in settings first.")
                }
                return@launch
            }

            val allNotes = repository.getAllNotesSnapshot()

            val success = BackupUtils.exportNotesToDirectory(
                getApplication(), uri, allNotes
            )

            withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (success) onSuccess() else onError("Failed to create backup.")
            }
        }
    }

    fun processAiAction(action: String, content: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val key = apiKey.value?.takeIf { it.isNotBlank() }
            if (key == null) {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult("Error: API Key not set in Settings → Intelligence")
                }
                return@launch
            }
            if (content.isBlank()) {
                withContext(kotlinx.coroutines.Dispatchers.Main) { onResult("Error: Note is empty") }
                return@launch
            }
            val model = modelId.value
            val systemPrompt = when (action) {
                "Grammar & Clarity" -> "You are a writing assistant. Fix grammar, punctuation and clarity. Keep meaning, return only the improved text."
                "Formal Tone" -> "Rewrite the following text in a formal, professional tone. Return only the rewritten text."
                "Casual Tone" -> "Rewrite the following text in a casual, friendly tone. Return only the rewritten text."
                "Bullets to Prose" -> "Convert the following bullet points into coherent prose paragraphs. Return only prose."
                "Prose to Bullets" -> "Convert the following prose into a concise bulleted list. Use • for bullets. Return only the list."
                "Q&A" -> "Answer strictly from the provided context. If the answer isn't in the context, say so. Be concise."
                else -> "Summarize the following note in 3-5 concise bullets. Return only the summary."
            }

            try {
                val response = NetworkModule.api.getCompletion(
                    apiKey = "Bearer $key",
                    request = ChatRequest(
                        model = model,
                        messages = listOf(
                            Message("system", systemPrompt),
                            Message("user", content)
                        )
                    )
                )
                val result = response.choices.firstOrNull()?.message?.content?.trim()?.takeIf { it.isNotBlank() }
                    ?: "Error: Empty response"
                withContext(kotlinx.coroutines.Dispatchers.Main) { onResult(result) }
            } catch (e: Exception) {
                val msg = when {
                    e is java.net.UnknownHostException -> "No internet"
                    e.message?.contains("401") == true -> "Invalid API key (401)"
                    e.message?.contains("429") == true -> "Rate limited (429) — try later"
                    else -> e.localizedMessage ?: "Unknown error"
                }
                withContext(kotlinx.coroutines.Dispatchers.Main) { onResult("Error: $msg") }
            }
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun setSelectedTab(tab: NoteTab) {
        _selectedTab.value = tab
    }

    /**
     * Unified method for adding or updating a note.
     * 
     * @param id The ID of the note to update, or null for a new note.
     * @param color The new color to set. If null, the existing color is maintained for updates.
     *              For new notes, null results in no color.
     */
    suspend fun saveNote(
        id: Int?,
        title: String,
        content: String,
        images: List<String> = emptyList(),
        isChecklist: Boolean = false,
        checklist: List<com.jusdots.jusnotes.data.ChecklistItem> = emptyList(),
        isSecret: Boolean? = null,
        isPinned: Boolean? = null,
        linkedNoteIds: List<Int>? = null,
        tags: List<String>? = null,
        color: Int? = null,
        clearColor: Boolean = false,
        isMap: Boolean? = null,
        mapItems: List<com.jusdots.jusnotes.data.MapItem>? = null
    ): Int {
        val now = System.currentTimeMillis()
        val note = if (id != null && id != -1) {
            val existing = repository.getNoteById(id) ?: return -1
            existing.copy(
                title = title,
                content = content,
                images = images,
                isChecklist = isChecklist,
                checklist = checklist,
                isSecret = isSecret ?: existing.isSecret,
                isPinned = isPinned ?: existing.isPinned,
                lastModified = now,
                linkedNoteIds = linkedNoteIds ?: existing.linkedNoteIds,
                tags = tags ?: existing.tags,
                color = if (clearColor) null else (color ?: existing.color),
                isMap = isMap ?: existing.isMap,
                mapItems = mapItems ?: existing.mapItems
            )
        } else {
            Note(
                title = title,
                content = content,
                images = images,
                isChecklist = isChecklist,
                checklist = checklist,
                isSecret = isSecret ?: false,
                isPinned = isPinned ?: false,
                timestamp = now,
                lastModified = now,
                linkedNoteIds = linkedNoteIds ?: emptyList(),
                tags = tags ?: emptyList(),
                color = color,
                isMap = isMap ?: false,
                mapItems = mapItems ?: emptyList()
            )
        }
        val insertedId = repository.insertNote(note)
        return if (id != null && id != -1) id else insertedId.toInt()
    }

    /**
     * Helper to launch a note save from the UI without managing coroutines.
     * Consolidates add/save logic.
     */
    fun addNote(
        title: String,
        content: String,
        images: List<String> = emptyList(),
        isChecklist: Boolean = false,
        checklist: List<com.jusdots.jusnotes.data.ChecklistItem> = emptyList(),
        isSecret: Boolean = false,
        isPinned: Boolean = false,
        isMap: Boolean = false,
        mapItems: List<com.jusdots.jusnotes.data.MapItem> = emptyList()
    ) {
        viewModelScope.launch {
            saveNote(
                id = null,
                title = title,
                content = content,
                images = images,
                isChecklist = isChecklist,
                checklist = checklist,
                isSecret = isSecret,
                isPinned = isPinned,
                isMap = isMap,
                mapItems = mapItems
            )
        }
    }

    fun createMapNote(title: String) {
        addNote(
            title = title,
            content = "",
            isMap = true
        )
    }

    fun updateMapItems(noteId: Int, items: List<com.jusdots.jusnotes.data.MapItem>) {
        viewModelScope.launch {
            val note = repository.getNoteById(noteId)
            if (note != null) {
                repository.insertNote(note.copy(mapItems = items, lastModified = System.currentTimeMillis()))
            }
        }
    }

    fun updateNoteTitle(id: Int, newTitle: String) {
        viewModelScope.launch {
            val note = repository.getNoteById(id)
            if (note != null) {
                repository.insertNote(note.copy(title = newTitle, lastModified = System.currentTimeMillis()))
            }
        }
    }

    fun toggleNotePinnedStatus(note: Note) {
        viewModelScope.launch {
            repository.insertNote(note.copy(isPinned = !note.isPinned))
        }
    }


    fun toggleNoteSecretStatus(note: Note) {
        viewModelScope.launch {
            repository.insertNote(note.copy(isSecret = !note.isSecret))
        }
    }

    // ─── Trash (Soft Delete) ────────────────────────────────────

    /**
     * Moves a note to the trash (soft delete).
     */
    fun trashNote(note: Note) {
        viewModelScope.launch {
            repository.softDeleteNote(note.id, System.currentTimeMillis())
        }
    }

    /**
     * Moves a note to the trash by its ID. Useful when the full Note object isn't available.
     */
    fun trashNoteById(id: Int) {
        viewModelScope.launch {
            repository.softDeleteNote(id, System.currentTimeMillis())
        }
    }

    fun restoreNote(note: Note) {
        viewModelScope.launch {
            repository.restoreNote(note.id)
        }
    }

    fun permanentlyDeleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
        }
    }

    // ─── Duplicate Note ─────────────────────────────────────────

    fun duplicateNote(note: Note) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.insertNote(
                note.copy(
                    id = 0,
                    title = "${note.title} (Copy)",
                    timestamp = now,
                    lastModified = now
                )
            )
        }
    }

    // ─── Import Notes ───────────────────────────────────────────

    fun importNotes(uris: List<Uri>, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val notes = BackupUtils.importNotesFromUris(getApplication(), uris)
            if (notes.isNotEmpty()) {
                repository.insertNotes(notes)
            }
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                onResult(notes.size)
            }
        }
    }

    // ─── Notes Connect (Linking) ────────────────────────────────

    suspend fun getNoteById(id: Int): Note? {
        return repository.getNoteById(id)
    }

    suspend fun getNoteByTitle(title: String): Note? {
        return repository.getNoteByTitle(title)
    }

    suspend fun getLinkedNotes(noteId: Int): List<Note> {
        val note = repository.getNoteById(noteId) ?: return emptyList()
        if (note.linkedNoteIds.isEmpty()) return emptyList()
        return repository.getNotesByIds(note.linkedNoteIds)
    }

    suspend fun getBacklinks(noteId: Int): List<Note> {
        // Query database directly to ensure fresh snapshot
        return repository.getAllNotesSnapshot()
            .filter { noteId in it.linkedNoteIds && it.id != noteId }
    }
}
