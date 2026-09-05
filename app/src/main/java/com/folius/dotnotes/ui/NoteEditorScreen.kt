package com.folius.dotnotes.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.folius.dotnotes.ui.components.BlurText
import com.folius.dotnotes.utils.MarkdownRenderer
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import coil.compose.AsyncImage
import com.folius.dotnotes.data.ChecklistItem
import com.folius.dotnotes.data.Note
import com.folius.dotnotes.ui.components.*
import kotlinx.coroutines.*
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

// Note color presets
val NoteColors = listOf(
    null to "None",
    0xFFEF5350.toInt() to "Red",
    0xFFFF7043.toInt() to "Orange",
    0xFFFFCA28.toInt() to "Yellow",
    0xFF66BB6A.toInt() to "Green",
    0xFF26A69A.toInt() to "Teal",
    0xFF42A5F5.toInt() to "Blue",
    0xFFAB47BC.toInt() to "Purple",
    0xFFEC407A.toInt() to "Pink"
)

// Helper for resolving [[Wiki Links]] to Note IDs
private suspend fun resolveWikiLinks(content: String, viewModel: NoteViewModel): List<Int> {
    val regex = Regex("\\[\\[(.*?)\\]\\]")
    return regex.findAll(content)
        .map { it.groupValues[1] }
        .toSet()
        .mapNotNull { title -> viewModel.getNoteByTitle(title)?.id }
        .toList()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditorScreen(
    viewModel: NoteViewModel,
    noteId: Int?,
    initialTab: String = "NOTES",
    onBack: () -> Unit,
    onImageClick: (String) -> Unit,
    onNavigateToNote: ((Int) -> Unit)? = null
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf(TextFieldValue("")) }
    var images by remember { mutableStateOf<List<String>>(emptyList()) }
    var isChecklist by remember { mutableStateOf(initialTab == "CHECKLISTS") }
    val checklist = remember { mutableStateListOf<ChecklistItem>() }
    var isSecret by remember { mutableStateOf(false) }
    var isPinned by remember { mutableStateOf(false) }
    var currentNoteId by remember { mutableStateOf(noteId) }
    var linkedNoteIds by remember { mutableStateOf<List<Int>>(emptyList()) }
    var tags by remember { mutableStateOf<List<String>>(emptyList()) }
    var noteColor by remember { mutableStateOf<Int?>(null) }
    var isMap by remember { mutableStateOf(initialTab == "MAPS") }
    var mapItems by remember { mutableStateOf<List<com.folius.dotnotes.data.MapItem>>(emptyList()) }
    
    var isSummarizing by remember { mutableStateOf(false) }
    var existingNote by remember { mutableStateOf<Note?>(null) }
    var showAiChat by remember { mutableStateOf(false) }
    var aiResponse by remember { mutableStateOf("") }
    var showMarkdownPreview by remember { mutableStateOf(false) }
    var showTagInput by remember { mutableStateOf(false) }
    var newTagText by remember { mutableStateOf("") }
    var showColorPicker by remember { mutableStateOf(false) }
    var isTitleExpanded by remember { mutableStateOf(false) }
    
    val animationsEnabled by viewModel.isAnimationsEnabled.collectAsState()
    
    var showTitleAnimation by remember { mutableStateOf(animationsEnabled && title.isNotBlank()) }
    var showContentAnimation by remember { mutableStateOf(animationsEnabled && content.text.isNotBlank()) }
    
    var initialTitle by remember { mutableStateOf("") }
    var initialContent by remember { mutableStateOf("") }
    var initialImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var initialIsChecklist by remember { mutableStateOf(false) }
    var initialChecklist by remember { mutableStateOf<List<ChecklistItem>>(emptyList()) }
    var initialIsSecret by remember { mutableStateOf(false) }
    var initialIsPinned by remember { mutableStateOf(false) }
    var initialIsMap by remember { mutableStateOf(false) }
    var initialMapItems by remember { mutableStateOf<List<com.folius.dotnotes.data.MapItem>>(emptyList()) }
    var initialTags by remember { mutableStateOf<List<String>>(emptyList()) }
    var initialNoteColor by remember { mutableStateOf<Int?>(null) }
    var initialLinkedNoteIds by remember { mutableStateOf<List<Int>>(emptyList()) }

    var isPillVisible by remember { mutableStateOf(true) }
    var showMapSelectionDialog by remember { mutableStateOf(false) }
    val mapNotes by viewModel.mapNotes.collectAsState()
    var isPillMenuExpanded by remember { mutableStateOf(false) }
    var undoArrowTick by remember { mutableStateOf(false) }
    var redoArrowTick by remember { mutableStateOf(false) }
    val undoArrowScale by animateFloatAsState(targetValue = if (undoArrowTick) 1.2f else 0.8f, animationSpec = spring(dampingRatio = 0.45f, stiffness = 500f), label = "undoS")
    val undoArrowAlpha by animateFloatAsState(targetValue = if (undoArrowTick) 1f else 0f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f), label = "undoA")
    val redoArrowScale by animateFloatAsState(targetValue = if (redoArrowTick) 1.2f else 0.8f, animationSpec = spring(dampingRatio = 0.45f, stiffness = 500f), label = "redoS")
    val redoArrowAlpha by animateFloatAsState(targetValue = if (redoArrowTick) 1f else 0f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f), label = "redoA")
    
    val hasShownHints by viewModel.hasShownGestureHints.collectAsState()
    var showHint by remember { mutableStateOf(false) }

    // Onboarding hints
    LaunchedEffect(hasShownHints) {
        if (!hasShownHints) {
            // Wait a bit after screen opens
            kotlinx.coroutines.delay(1000)
            showHint = true
            // Save state immediately or after a delay
            viewModel.setHasShownGestureHints(true)
        }
    }

    if (showHint) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(5000)
            showHint = false
        }
    }
    
    val context = LocalContext.current
    
    val hasUnsavedChanges = title != initialTitle || 
                            content.text != initialContent || 
                            images != initialImages || 
                            isChecklist != initialIsChecklist || 
                            checklist.toList() != initialChecklist ||
                            isSecret != initialIsSecret ||
                            isPinned != initialIsPinned ||
                            tags != initialTags ||
                            noteColor != initialNoteColor ||
                            isMap != initialIsMap ||
                            linkedNoteIds != initialLinkedNoteIds
    
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val pinnedNotes by viewModel.pinnedNotes.collectAsState()
    val keyboardVisible by rememberUpdatedState(WindowInsets.isImeVisible)
    
    // Markdown Helper Functions
    fun insertMarkdown(prefix: String, suffix: String = "") {
        val selection = content.selection
        val text = content.text
        
        val newText = text.substring(0, selection.start) + 
                     prefix + text.substring(selection.start, selection.end) + suffix +
                     text.substring(selection.end)
        
        val newSelection = if (selection.collapsed) {
            TextRange(selection.start + prefix.length)
        } else {
            TextRange(selection.start, selection.end + prefix.length + suffix.length)
        }
        
        content = content.copy(text = newText, selection = newSelection)
    }
    
    fun insertLinePrefix(prefix: String) {
        val text = content.text
        val selection = content.selection
        
        // Find the start of the current line
        var lineStart = selection.start
        while (lineStart > 0 && text[lineStart - 1] != '\n') {
            lineStart--
        }
        
        val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
        val newSelection = TextRange(selection.start + prefix.length, selection.end + prefix.length)
        
        content = content.copy(text = newText, selection = newSelection)
    }

    // --- Undo/Redo State ---
    val undoStack = remember { mutableStateListOf<Pair<String, TextFieldValue>>() }
    val redoStack = remember { mutableStateListOf<Pair<String, TextFieldValue>>() }
    var lastRecordedTitle by remember { mutableStateOf(title) }
    var lastRecordedContent by remember { mutableStateOf(content) }

    fun pushToHistory() {
        if (title != lastRecordedTitle || content.text != lastRecordedContent.text) {
            undoStack.add(lastRecordedTitle to lastRecordedContent)
            if (undoStack.size > 50) undoStack.removeAt(0)
            redoStack.clear()
            lastRecordedTitle = title
            lastRecordedContent = content
        }
    }

    fun handleUndo() {
        if (undoStack.isNotEmpty()) {
            val (uTitle, uContent) = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(title to content)
            title = uTitle
            content = uContent
            lastRecordedTitle = uTitle
            lastRecordedContent = uContent
            Toast.makeText(context, "Undo", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleRedo() {
        if (redoStack.isNotEmpty()) {
            val (rTitle, rContent) = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(title to content)
            title = rTitle
            content = rContent
            lastRecordedTitle = rTitle
            lastRecordedContent = rContent
            Toast.makeText(context, "Redo", Toast.LENGTH_SHORT).show()
        }
    }

    // Debounced history recording
    LaunchedEffect(title, content) {
        delay(1000)
        pushToHistory()
    }

    // â”€â”€â”€ Word Count â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    val wordCount = remember(content.text, checklist, isChecklist) {
        val text = if (isChecklist) {
            checklist.joinToString(" ") { it.text }
        } else {
            content.text
        }
        if (text.isBlank()) 0 else text.trim().split(Regex("\\s+")).size
    }
    val charCount = remember(content.text, checklist, isChecklist) {
        if (isChecklist) {
            checklist.sumOf { it.text.length }
        } else {
            content.text.length
        }
    }

    // â”€â”€â”€ Autosave (increased debounce to 1000ms for perf) â”€â”€â”€
    LaunchedEffect(title, content.text, images, isChecklist, checklist, isSecret, isPinned, tags, noteColor, isMap, mapItems) {
        if (title.isBlank() && content.text.isBlank() && checklist.isEmpty() && images.isEmpty() && !isMap) return@LaunchedEffect
        
        delay(1000) // Debounce 1s for efficiency
        
        linkedNoteIds = resolveWikiLinks(content.text, viewModel)
        
        val newId = viewModel.saveNote(
            id = currentNoteId,
            title = title,
            content = content.text,
            images = images,
            isChecklist = isChecklist,
            checklist = checklist,
            isSecret = isSecret,
            isPinned = isPinned,
            linkedNoteIds = linkedNoteIds,
            tags = tags,
            color = noteColor,
            clearColor = noteColor == null && currentNoteId != null && currentNoteId != -1,
            isMap = isMap,
            mapItems = mapItems
        )
        if (currentNoteId == null || currentNoteId == -1) {
            currentNoteId = newId
        }
    }

    LaunchedEffect(noteId) {
        if (noteId != null && noteId != -1) {
            val note = viewModel.getNoteById(noteId)
            if (note != null) {
                existingNote = note
                title = note.title
                content = TextFieldValue(note.content)
                images = note.images
                isChecklist = note.isChecklist
                checklist.clear()
                checklist.addAll(note.checklist)
                isSecret = note.isSecret
                isPinned = note.isPinned
                isMap = note.isMap
                mapItems = note.mapItems
                linkedNoteIds = note.linkedNoteIds
                tags = note.tags
                noteColor = note.color
                
                initialTitle = note.title
                initialContent = note.content
                initialImages = note.images
                initialIsChecklist = note.isChecklist
                initialChecklist = note.checklist
                initialIsSecret = note.isSecret
                initialIsPinned = note.isPinned
                initialIsMap = note.isMap
                initialMapItems = note.mapItems
                initialTags = note.tags
                initialNoteColor = note.color
                initialLinkedNoteIds = note.linkedNoteIds

                if (animationsEnabled) {
                    showTitleAnimation = note.title.isNotBlank()
                    showContentAnimation = note.content.isNotBlank()
                }
            }
        }
    }

    // Stability: Animation auto-dismiss logic hoited out of code branches
    LaunchedEffect(showTitleAnimation) {
        if (!showTitleAnimation || title.isBlank()) return@LaunchedEffect
        val elements = title.split(Regex("(?<=\\s)|(?=\\s)")).filter { it.isNotEmpty() }
        delay((elements.size * 200L) + 1000L)
        showTitleAnimation = false
    }

    LaunchedEffect(showContentAnimation) {
        if (!showContentAnimation || content.text.isBlank()) return@LaunchedEffect
        val elements = content.text.split(Regex("(?<=\\s)|(?=\\s)")).filter { it.isNotEmpty() }
        delay((elements.size * 20L) + 1000L)
        showContentAnimation = false
    }


    if (isMap && currentNoteId != null && currentNoteId != -1) {
        NoteMapScreen(
            noteId = currentNoteId!!,
            viewModel = viewModel,
            mapItems = mapItems,
            onMapItemsChange = { mapItems = it },
            onNavigateToNote = { id -> onNavigateToNote?.invoke(id) },
            onBack = onBack
        )
        return
    }

    // Load Backlinks
    var backlinks by remember { mutableStateOf<List<Note>>(emptyList()) }
    LaunchedEffect(currentNoteId) {
        if (currentNoteId != null && currentNoteId != -1) {
            backlinks = viewModel.getBacklinks(currentNoteId!!)
        }
    }


    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        val newImages = uris.map { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                    android.util.Log.w("NoteEditor", "Could not persist URI permission", e)
                }
            uri.toString()
        }
        images = images + newImages
    }

    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { images = images + it.toString() }
        }
    }

    val attemptBack: () -> Unit = {
        if (hasUnsavedChanges) {
            scope.launch {
                linkedNoteIds = resolveWikiLinks(content.text, viewModel)
                viewModel.saveNote(
                    id = currentNoteId,
                    title = title,
                    content = content.text,
                    images = images,
                    isChecklist = isChecklist,
                    checklist = checklist,
                    isSecret = isSecret,
                    isPinned = isPinned,
                    linkedNoteIds = linkedNoteIds,
                    tags = tags,
                    color = noteColor,
                    clearColor = noteColor == null && currentNoteId != null && currentNoteId != -1,
                    isMap = isMap,
                    mapItems = mapItems
                )
                onBack()
            }
        } else {
            onBack()
        }
    }

    PredictiveBackHandler(enabled = hasUnsavedChanges) { progress ->
        try {
            progress.collect { /* No-op */ }
            attemptBack()
        } catch (e: Exception) {
            android.util.Log.e("NoteEditor", "Predictive back failed", e)
        }
    }


    // --- Minimal redesign to match mock: dark outer, rounded content card, pill title ---
    val outerBg = MaterialTheme.colorScheme.background
    val cardBg = MaterialTheme.colorScheme.surfaceContainer
    val pillBg = MaterialTheme.colorScheme.surfaceContainerHighest
    val contentPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    Scaffold(
        containerColor = outerBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(outerBg)
                .imePadding()
                .pointerInput(Unit) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (totalDrag > 300) { attemptBack() }
                            totalDrag = 0f
                        },
                        onDragCancel = { totalDrag = 0f },
                        onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount }
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Content card — matches Image 1: large rounded rectangle filling screen
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = cardBg,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(0.dp)
                    ) {
                        if (isChecklist) {
                            val lazyState = androidx.compose.foundation.lazy.rememberLazyListState()
                            val reorderState = rememberReorderableLazyListState(lazyState) { from, to ->
                                checklist.add(to.index, checklist.removeAt(from.index))
                            }
                            androidx.compose.foundation.lazy.LazyColumn(
                                state = lazyState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp, vertical = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(checklist, key = { it.id }) { item ->
                                    ReorderableItem(reorderState, key = item.id) { isDragging ->
                                        val elevation = if (isDragging) 8.dp else 0.dp
                                        Surface(shadowElevation = elevation, color = Color.Transparent) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Checkbox(
                                                    checked = item.isChecked,
                                                    onCheckedChange = { checked ->
                                                        val idx = checklist.indexOfFirst { it.id == item.id }
                                                        if (idx != -1) checklist[idx] = item.copy(isChecked = checked)
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = MaterialTheme.colorScheme.onSurface,
                                                        uncheckedColor = contentPlaceholderColor,
                                                        checkmarkColor = cardBg
                                                    )
                                                )
                                                androidx.compose.foundation.text.BasicTextField(
                                                    value = item.text,
                                                    onValueChange = { nv ->
                                                        val idx = checklist.indexOfFirst { it.id == item.id }
                                                        if (idx != -1) checklist[idx] = item.copy(text = nv)
                                                    },
                                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                                        color = if (item.isChecked) Color.White.copy(alpha = 0.5f) else Color.White
                                                    ),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(vertical = 8.dp)
                                                )
                                                IconButton(onClick = {}, modifier = Modifier.draggableHandle()) {
                                                    Icon(Icons.Default.DragHandle, null, tint = contentPlaceholderColor)
                                                }
                                                IconButton(onClick = { checklist.removeIf { it.id == item.id } }) {
                                                    Icon(Icons.Default.Close, null, tint = contentPlaceholderColor, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                                item {
                                    TextButton(onClick = { checklist.add(ChecklistItem(text = "")) }) {
                                        Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Add Item", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        } else if (showMarkdownPreview) {
                            val cs = MaterialTheme.colorScheme
                            val blocks = remember(content.text) {
                                MarkdownRenderer.parseBlocks(
                                    markdown = content.text,
                                    linkColor = Color(0xFF9AB3FF),
                                    codeColor = Color.White,
                                    codeBackground = Color.White.copy(alpha = 0.08f)
                                )
                            }
                            androidx.compose.foundation.lazy.LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(blocks.size) { idx ->
                                    MarkdownBlockView(
                                        block = blocks[idx],
                                        onNavigateToNote = { id -> onNavigateToNote?.invoke(id) },
                                        onWikiLinkClick = { title ->
                                            scope.launch {
                                                val n = viewModel.getNoteByTitle(title)
                                                if (n != null) onNavigateToNote?.invoke(n.id)
                                                else Toast.makeText(context, "Note \"$title\" not found", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                            }
                        } else {
                            // Images row at top of card if present
                            Column(modifier = Modifier.fillMaxSize()) {
                                if (images.isNotEmpty()) {
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(images) { uri ->
                                            Box {
                                                AsyncImage(
                                                    model = uri,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).clickable { onImageClick(uri) },
                                                    contentScale = ContentScale.Crop
                                                )
                                                IconButton(onClick = { images = images - uri }, modifier = Modifier.align(Alignment.TopEnd).size(20.dp)) {
                                                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxWidth()
                                ) {
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = content,
                                        onValueChange = { nv ->
                                            if (nv.text.endsWith("[[") && !content.text.endsWith("[[")) {
                                                Toast.makeText(context, "Wiki Link: Type note title inside [[]]", Toast.LENGTH_SHORT).show()
                                            }
                                            if (nv.text.startsWith("[] ") || nv.text.startsWith("- [] ") || nv.text.startsWith("- [ ] ")) {
                                                if (content.text.isBlank() || nv.text.length <= 6) {
                                                    isChecklist = true
                                                    content = TextFieldValue("")
                                                    return@BasicTextField
                                                }
                                            }
                                            content = nv
                                        },
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface, lineHeight = 24.sp),
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 24.dp).verticalScroll(rememberScrollState()),
                                        cursorBrush = SolidColor(Color.White)
                                    )
                                    if (content.text.isEmpty()) {
                                        Text(
                                            text = "Content\nabcdefghijklmnop\nqrstuvwxyz",
                                            style = MaterialTheme.typography.bodyLarge.copy(color = contentPlaceholderColor, lineHeight = 24.sp),
                                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)
                                        )
                                    }
                                }
                                if (backlinks.isNotEmpty()) {
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 20.dp))
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        backlinks.forEach { note ->
                                            AssistChip(
                                                onClick = { onNavigateToNote?.invoke(note.id) },
                                                label = { Text(if (note.title.isNotBlank()) note.title else "Untitled", color = Color.White, style = MaterialTheme.typography.labelSmall) },
                                                leadingIcon = { Icon(Icons.Default.Link, null, modifier = Modifier.size(14.dp), tint = Color.White) },
                                                colors = AssistChipDefaults.assistChipColors(containerColor = Color.White.copy(alpha = 0.12f)),
                                                border = null
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title pill — flat integrated menu (Image 1 base, tap to expand, still not floating overlay)
                val pillHeight by animateDpAsState(targetValue = if (isPillMenuExpanded) 420.dp else 56.dp, animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f), label = "pillH")
                val pillCorner by animateDpAsState(targetValue = if (isPillMenuExpanded) 32.dp else 28.dp, animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f), label = "pillCorner")
                val pillShape = RoundedCornerShape(pillCorner)
                Surface(
                    shape = pillShape,
                    color = noteColor?.let { Color(it) } ?: pillBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(pillHeight)
                        .pointerInput(isPillMenuExpanded) {
                            var totalX = 0f
                            var totalY = 0f
                            detectDragGestures(
                                onDragEnd = {
                                    if (isPillMenuExpanded && totalY > 80) isPillMenuExpanded = false
                                    else if (!isPillMenuExpanded && totalY < -80) isPillMenuExpanded = true
                                    else if (!isPillMenuExpanded && totalX > 100) {
                                        undoArrowTick = true; scope.launch { kotlinx.coroutines.delay(600); undoArrowTick = false }; handleUndo()
                                    } else if (!isPillMenuExpanded && totalX < -100) {
                                        redoArrowTick = true; scope.launch { kotlinx.coroutines.delay(600); redoArrowTick = false }; handleRedo()
                                    }
                                    totalX = 0f; totalY = 0f
                                },
                                onDragCancel = { totalX = 0f; totalY = 0f },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    totalX += dragAmount.x
                                    totalY += dragAmount.y
                                }
                            )
                        }
                        .combinedClickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                            onLongClick = {
                                scope.launch {
                                    linkedNoteIds = resolveWikiLinks(content.text, viewModel)
                                    viewModel.saveNote(
                                        id = currentNoteId,
                                        title = title,
                                        content = content.text,
                                        images = images,
                                        isChecklist = isChecklist,
                                        checklist = checklist,
                                        isSecret = isSecret,
                                        isPinned = isPinned,
                                        linkedNoteIds = linkedNoteIds,
                                        tags = tags,
                                        color = noteColor,
                                        clearColor = noteColor == null && currentNoteId != null && currentNoteId != -1,
                                        isMap = isMap,
                                        mapItems = mapItems
                                    )
                                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onClick = { isPillMenuExpanded = !isPillMenuExpanded; isTitleExpanded = false }
                        )
                ) {
                    androidx.compose.animation.AnimatedContent(targetState = isPillMenuExpanded, label = "pillMenu") { expanded ->
                        if (expanded) {
                            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp).verticalScroll(rememberScrollState())) {
                                Box(modifier = Modifier.width(28.dp).height(3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)).align(Alignment.CenterHorizontally))
                                Spacer(modifier = Modifier.height(18.dp))
                                Text("Note actions", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxWidth())
                                Text("Tap an action • swipe pill to undo/redo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 14.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f))
                                Spacer(modifier = Modifier.height(6.dp))
                                val menuItems = listOf(
                                    Triple(Icons.Default.Palette, "Color", { isPillMenuExpanded = false; showColorPicker = true }),
                                    Triple(Icons.Default.Label, "Tags", { isPillMenuExpanded = false; showTagInput = true }),
                                    Triple(if (showMarkdownPreview) Icons.Default.EditNote else Icons.Default.Preview, if (showMarkdownPreview) "Edit" else "Preview", { isPillMenuExpanded = false; showMarkdownPreview = !showMarkdownPreview }),
                                    Triple(if (isSecret) Icons.Default.LockOpen else Icons.Default.Lock, if (isSecret) "Unlock" else "Lock", { isPillMenuExpanded = false; isSecret = !isSecret }),
                                    Triple(if (isPinned) Icons.Default.PushPin else Icons.Default.BookmarkBorder, if (isPinned) "Unpin" else "Pin", { isPillMenuExpanded = false; isPinned = !isPinned }),
                                    Triple(Icons.Default.Share, "Share", {
                                        isPillMenuExpanded = false
                                        val i = android.content.Intent().apply { action = android.content.Intent.ACTION_SEND; putExtra(android.content.Intent.EXTRA_TEXT, "$title\n\n${content.text}"); type = "text/plain" }
                                        context.startActivity(android.content.Intent.createChooser(i, "Share Note"))
                                    }),
                                    Triple(Icons.Default.Image, "Image", { isPillMenuExpanded = false; imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }),
                                    Triple(if (isChecklist) Icons.Default.Notes else Icons.Default.CheckBox, if (isChecklist) "Text" else "Checklist", { isPillMenuExpanded = false; if (!isChecklist) { if (content.text.isNotBlank()) { val n = content.text.split("\n").filter{it.isNotBlank()}.map{ ChecklistItem(text=it.trim()) }; checklist.clear(); checklist.addAll(n) }; content = TextFieldValue("") } else { if (checklist.isNotEmpty()) { val nt = checklist.joinToString("\n"){ (if(it.isChecked)"- [x] " else "- [ ] ")+it.text }; content = TextFieldValue(nt, selection=TextRange(nt.length)) }; checklist.clear() }; isChecklist=!isChecklist }),
                                    Triple(Icons.Default.ContentCopy, "Duplicate", { isPillMenuExpanded = false; scope.launch { val n=viewModel.getNoteById(currentNoteId?:-1); n?.let{viewModel.duplicateNote(it)}; Toast.makeText(context,"Duplicated",Toast.LENGTH_SHORT).show() } }),
                                    Triple(Icons.Default.AutoAwesome, "AI Assist", { isPillMenuExpanded = false; showAiChat = true }),
                                    Triple(Icons.Default.DeleteOutline, "Delete", { isPillMenuExpanded = false; val id=currentNoteId?:existingNote?.id; id?.let{viewModel.trashNoteById(it)}; onBack() })
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    menuItems.forEachIndexed { index, item ->
                                        val icon = item.first
                                        val label = item.second
                                        val onClick = item.third
                                        val isDestructive = label == "Delete"
                                        val shape = when {
                                            menuItems.size <= 1 -> androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
                                            index == 0 -> androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
                                            index == menuItems.size - 1 -> androidx.compose.foundation.shape.RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
                                            else -> androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                                        }
                                        androidx.compose.material3.Card(
                                            shape = shape,
                                            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                            elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp),
                                            modifier = Modifier.fillMaxWidth().clickable { onClick() }
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(if (isDestructive) MaterialTheme.colorScheme.error.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                                            Icon(icon, null, tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                        Icon(Icons.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f), modifier = Modifier.size(18.dp))
                                    }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)).padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                                    Text("Long-press pill to save  •  Swipe to undo/redo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Box(modifier = Modifier.size(28.dp).graphicsLayer(scaleX = undoArrowScale, scaleY = undoArrowScale, alpha = undoArrowAlpha).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                    Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                                        androidx.compose.foundation.text.BasicTextField(
                                            value = title,
                                            onValueChange = { title = it },
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.titleMedium.copy(color=MaterialTheme.colorScheme.onSurface, textAlign=androidx.compose.ui.text.style.TextAlign.Center, fontWeight=FontWeight.Medium),
                                            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.onSurface),
                                            modifier = Modifier.fillMaxWidth(),
                                            decorationBox = { inner -> Box(contentAlignment=Alignment.Center, modifier=Modifier.fillMaxWidth()) { if (title.isEmpty()) Text("title", style=MaterialTheme.typography.titleMedium.copy(color=MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.8f), textAlign=androidx.compose.ui.text.style.TextAlign.Center, fontWeight=FontWeight.Medium)); inner() } }
                                        )
                                    }
                                    Box(modifier = Modifier.size(28.dp).graphicsLayer(scaleX = redoArrowScale, scaleY = redoArrowScale, alpha = redoArrowAlpha).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }




        // Color Picker Dialog
        if (showColorPicker) {
            AlertDialog(
                onDismissRequest = { showColorPicker = false },
                title = { Text("Note Color") },
                text = {
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NoteColors.forEach { (color, name) ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(MaterialTheme.shapes.large)
                                    .background(color?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        width = if (noteColor == color) 2.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = MaterialTheme.shapes.large,
                                    )
                                    .clickable {
                                        noteColor = color
                                        showColorPicker = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (color == null) {
                                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showColorPicker = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Tag Input Dialog
        if (showTagInput) {
            AlertDialog(
                onDismissRequest = { showTagInput = false },
                title = { Text("Tags") },
                text = {
                    Column {
                        // Existing tags
                        if (tags.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                tags.forEach { tag ->
                                    InputChip(
                                        selected = false,
                                        onClick = { tags = tags - tag },
                                        label = { Text("#$tag") },
                                        trailingIcon = {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        OutlinedTextField(
                            value = newTagText,
                            onValueChange = { newTagText = it.replace(" ", "_") },
                            placeholder = { Text("Add a tag...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (newTagText.isNotBlank() && newTagText !in tags) {
                                            tags = tags + newTagText
                                            newTagText = ""
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add")
                                }
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTagInput = false }) {
                        Text("Done")
                    }
                }
            )
        }

        // Map Selection Dialog
        if (showMapSelectionDialog) {
            AlertDialog(
                onDismissRequest = { showMapSelectionDialog = false },
                title = { Text("Add to Map") },
                text = {
                    if (mapNotes.isEmpty()) {
                        Text("No map notes found. Create a map note first.")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                            items(mapNotes) { mapNote ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scope.launch {
                                                // 1. Ensure current note is saved to get ID
                                                val nid = viewModel.saveNote(
                                                    id = currentNoteId,
                                                    title = title,
                                                    content = content.text,
                                                    images = images,
                                                    isChecklist = isChecklist,
                                                    checklist = checklist,
                                                    isSecret = isSecret,
                                                    isPinned = isPinned,
                                                    linkedNoteIds = linkedNoteIds,
                                                    tags = tags,
                                                    color = noteColor,
                                                    isMap = isMap,
                                                    mapItems = mapItems
                                                )
                                                // 2. Add this note to the selected map
                                                if (nid != -1 && nid !in mapNote.linkedNoteIds) {
                                                    viewModel.saveNote(
                                                        id = mapNote.id,
                                                        title = mapNote.title,
                                                        content = mapNote.content,
                                                        linkedNoteIds = mapNote.linkedNoteIds + nid
                                                    )
                                                    Toast.makeText(context, "Added to ${mapNote.title}", Toast.LENGTH_SHORT).show()
                                                } else if (nid != -1) {
                                                    Toast.makeText(context, "Already in ${mapNote.title}", Toast.LENGTH_SHORT).show()
                                                }
                                                showMapSelectionDialog = false
                                            }
                                        }
                                        .padding(12.dp)
                                ) {
                                    Icon(Icons.Default.Map, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(mapNote.title)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showMapSelectionDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }





        // --- AI Assist Sheet ---
        if (showAiChat) {
            ModalBottomSheet(
                onDismissRequest = { showAiChat = false },
                sheetState = sheetState
            ) {
                var aiAction by remember { mutableStateOf("Summary") }
                var aiQuery by remember { mutableStateOf("") }
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                ) {
                    Text(
                        text = "AI Assistant",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        val actions = listOf("Summary", "Grammar & Clarity", "Formal Tone", "Casual Tone", "Bullets to Prose", "Prose to Bullets", "Q&A")
                        actions.forEach { action ->
                            InputChip(
                                selected = aiAction == action,
                                onClick = { aiAction = action },
                                label = { Text(action) }
                            )
                        }
                    }

                    if (aiAction == "Q&A") {
                        OutlinedTextField(
                            value = aiQuery,
                            onValueChange = { aiQuery = it },
                            placeholder = { Text("Ask something about this note...") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (content.text.isNotBlank() && aiQuery.isNotBlank()) {
                                        isSummarizing = true
                                        aiResponse = "Thinking..."
                                        viewModel.processAiAction("Q&A", "Context:\n${content.text}\n\nQuestion: $aiQuery") { result ->
                                            aiResponse = result ?: "Error: No response"
                                            isSummarizing = false
                                        }
                                    }
                                }) { Icon(Icons.Default.Send, null) }
                            }
                        )
                    } else {
                        Button(
                            onClick = {
                                if (content.text.isNotBlank()) {
                                    isSummarizing = true
                                    aiResponse = "Processing..."
                                    viewModel.processAiAction(aiAction, content.text) { result ->
                                        aiResponse = result ?: "Error: No response"
                                        isSummarizing = false
                                    }
                                } else {
                                    Toast.makeText(context, "Nothing to process", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            enabled = !isSummarizing
                        ) {
                            Text("Run $aiAction")
                        }
                    }
                    
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(min = 100.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (isSummarizing) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            Text(
                                text = aiResponse,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("AI Summary", aiResponse)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            enabled = !isSummarizing && aiResponse.isNotBlank() && !aiResponse.startsWith("Error") && !aiResponse.startsWith("Generating")
                        ) {
                            Icon(Icons.Default.ContentCopy, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copy")
                        }

                        Button(
                            onClick = {
                                val insertionText = "\n\n--- AI $aiAction ---\n$aiResponse"
                                val newText = content.text + insertionText
                                content = content.copy(text = newText, selection = TextRange(newText.length))
                                showAiChat = false
                                Toast.makeText(context, "Added to note", Toast.LENGTH_SHORT).show()
                            },
                            enabled = !isSummarizing && aiResponse.isNotBlank() && !aiResponse.startsWith("Error") && !aiResponse.startsWith("Generating") && !aiResponse.startsWith("Processing") && !aiResponse.startsWith("Thinking")
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Append")
                        }
                        
                        Button(
                            onClick = {
                                content = content.copy(text = aiResponse, selection = TextRange(aiResponse.length))
                                showAiChat = false
                                Toast.makeText(context, "Replaced note content", Toast.LENGTH_SHORT).show()
                            },
                            enabled = !isSummarizing && aiResponse.isNotBlank() && !aiResponse.startsWith("Error") && !aiResponse.startsWith("Generating") && !aiResponse.startsWith("Processing") && !aiResponse.startsWith("Thinking")
                        ) {
                            Icon(Icons.Default.Check, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Replace")
                        }
                    }
                }
            }
        }
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteContent(
    title: String,
    onTitleChange: (String) -> Unit,
    content: TextFieldValue,
    onContentChange: (TextFieldValue) -> Unit,
    images: List<String>,
    onImageRemove: (String) -> Unit,
    isChecklist: Boolean,
    onIsChecklistChange: (Boolean) -> Unit,
    checklist: SnapshotStateList<ChecklistItem>,
    isMap: Boolean,
    tags: List<String>,
    noteColor: Int?,
    onImageClick: (String) -> Unit,
    showMarkdownPreview: Boolean,
    onNavigateToNote: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
    noteId: Int?,
    showTitleAnimation: Boolean,
    showContentAnimation: Boolean,
    backlinks: List<Note>,
    attemptBack: () -> Unit,
    showTagInput: Boolean,
    onShowTagInput: (Boolean) -> Unit,
    scope: CoroutineScope,
    context: android.content.Context,
    viewModel: NoteViewModel,
    padding: PaddingValues
) {
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val colorScheme = MaterialTheme.colorScheme
    val blocks = remember(content.text, colorScheme, showMarkdownPreview) {
        if (showMarkdownPreview) {
            MarkdownRenderer.parseBlocks(
                markdown = content.text,
                linkColor = colorScheme.primary,
                codeColor = colorScheme.secondary,
                codeBackground = colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        } else emptyList()
    }

    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        checklist.add(to.index, checklist.removeAt(from.index))
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier
            .fillMaxSize()
            .padding(padding)
            .imePadding()
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (totalDrag > 300) { attemptBack() }
                        totalDrag = 0f
                    },
                    onDragCancel = { totalDrag = 0f },
                    onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount }
                )
            }
            .padding(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (noteId == null || noteId == -1) "New Note" else "Edit Note",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                noteColor?.let { c ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(c))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }

        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = title,
                    onValueChange = onTitleChange,
                    placeholder = { Text("Title", style = MaterialTheme.typography.titleLarge) },
                    modifier = Modifier.fillMaxWidth().alpha(if (showTitleAnimation && title.isNotBlank()) 0f else 1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.titleLarge
                )
                
                if (showTitleAnimation && title.isNotBlank()) {
                     BlurText(
                        text = title, 
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                     )
                }
            }
        }

        if (tags.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tags.forEach { tag ->
                        SuggestionChip(
                            onClick = { onShowTagInput(true) },
                            label = { Text("#$tag", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        if (images.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(images) { imageUri ->
                        Box {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(100.dp)
                                    .clickable { onImageClick(imageUri) },
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { onImageRemove(imageUri) },
                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        if (isChecklist) {
            items(checklist, key = { it.id }) { item ->
                ReorderableItem(reorderState, key = item.id) { isDragging ->
                    val elevation = if (isDragging) 8.dp else 0.dp
                    Surface(
                        shadowElevation = elevation,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = item.isChecked,
                                onCheckedChange = { isChecked ->
                                    val index = checklist.indexOfFirst { it.id == item.id }
                                    if (index != -1) {
                                        checklist[index] = item.copy(isChecked = isChecked)
                                    }
                                }
                            )
                            val strikethroughProgress by animateFloatAsState(
                                targetValue = if (item.isChecked) 1f else 0f,
                                animationSpec = spring(stiffness = Spring.StiffnessLow),
                                label = "strikethrough"
                            )
                            val colorScheme = MaterialTheme.colorScheme
                            
                            TextField(
                                value = item.text,
                                onValueChange = { newText ->
                                    val index = checklist.indexOfFirst { it.id == item.id }
                                    if (index != -1) {
                                        checklist[index] = item.copy(text = newText)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .drawBehind {
                                        if (strikethroughProgress > 0f) {
                                            val strokeY = this.size.height / 2f + 4.dp.toPx()
                                            drawLine(
                                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                start = Offset(0f, strokeY),
                                                end = Offset(this.size.width * strikethroughProgress, strokeY),
                                                strokeWidth = 2.dp.toPx(),
                                                cap = StrokeCap.Round
                                            )
                                        }
                                    },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = if (item.isChecked) colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else colorScheme.onSurface
                                )
                            )
                            IconButton(
                                onClick = {},
                                modifier = Modifier.draggableHandle()
                            ) {
                                Icon(Icons.Default.DragHandle, contentDescription = "Reorder", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = {
                                checklist.removeIf { it.id == item.id }
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Delete Item")
                            }
                        }
                    }
                }
            }
            item {
                TextButton(onClick = {
                    checklist.add(ChecklistItem(text = ""))
                }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Item")
                }
            }
        } else if (showMarkdownPreview) {
            items(blocks) { block ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    MarkdownBlockView(
                        block = block,
                        onNavigateToNote = { id -> onNavigateToNote?.invoke(id) },
                        onWikiLinkClick = { title ->
                            scope.launch {
                                val linkedNote = viewModel.getNoteByTitle(title)
                                if (linkedNote != null) onNavigateToNote?.invoke(linkedNote.id)
                                else Toast.makeText(context, "Note \"$title\" not found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        } else {
            item {
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp)) {
                    TextField(
                        value = content,
                        onValueChange = { newValue -> 
                            if (newValue.text.endsWith("[[") && !content.text.endsWith("[[")) {
                                Toast.makeText(context, "Wiki Link: Type note title inside [[]]", Toast.LENGTH_SHORT).show()
                            }
                            
                            // [] checklist conversion
                            if (newValue.text.startsWith("[] ") || newValue.text.startsWith("- [] ") || newValue.text.startsWith("- [ ] ")) {
                                if (content.text.isBlank() || newValue.text.length <= 6) {
                                    onIsChecklistChange(true)
                                    onContentChange(TextFieldValue(""))
                                    return@TextField
                                }
                            }
                            
                            onContentChange(newValue) 
                        },
                        placeholder = { Text("Add yo content here -Folius") },
                        modifier = Modifier.fillMaxWidth().alpha(if (showContentAnimation && content.text.isNotBlank()) 0f else 1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge
                    )
                     if (showContentAnimation && content.text.isNotBlank()) {
                           BlurText(
                              text = content.text, 
                              style = MaterialTheme.typography.bodyLarge,
                              modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                              delay = 20
                           )
                     }
                }
            }
        }

        if (backlinks.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Linked to this note:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    backlinks.forEach { note ->
                        AssistChip(
                            onClick = { onNavigateToNote?.invoke(note.id) },
                            label = { Text(if (note.title.isNotBlank()) note.title else "Untitled") },
                            leadingIcon = { 
                                Icon(
                                    Icons.Default.Link, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(14.dp)
                                ) 
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}



/*"Abracadabra, amor-oo-na-na
Abracadabra, morta-oo-ga-ga
Abracadabra, abra-oo-na-na"
In her tongue she said, "Death or love tonight"
(Oooo look at an easter egg!) */
