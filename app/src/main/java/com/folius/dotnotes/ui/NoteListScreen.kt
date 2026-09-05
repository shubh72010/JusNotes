package com.folius.dotnotes.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import com.folius.dotnotes.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.gestures.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import com.folius.dotnotes.data.Note
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.animation.core.exponentialDecay
import kotlin.math.*
import java.text.SimpleDateFormat
import java.util.*
import com.folius.dotnotes.ui.components.PinnedSubpill
import com.folius.dotnotes.ui.components.DotPill
import com.folius.dotnotes.ui.components.PinnedNotesBar
import com.folius.dotnotes.ui.components.TabSwitcherPill
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast

import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

private val GroupLargeCorner = 28.dp
private val GroupSmallCorner = 6.dp
private fun segmentedShape(index: Int, count: Int): androidx.compose.ui.graphics.Shape =
    when {
        count <= 1 -> RoundedCornerShape(GroupLargeCorner)
        index == 0 -> RoundedCornerShape(topStart = GroupLargeCorner, topEnd = GroupLargeCorner, bottomStart = GroupSmallCorner, bottomEnd = GroupSmallCorner)
        index == count - 1 -> RoundedCornerShape(topStart = GroupSmallCorner, topEnd = GroupSmallCorner, bottomStart = GroupLargeCorner, bottomEnd = GroupLargeCorner)
        else -> RoundedCornerShape(GroupSmallCorner)
    }

enum class DragAnchors {
    Settled,
    RevealedStart,
    DismissedStart,
    RevealedEnd,
    DismissedEnd,
    StartToEnd,
    EndToStart
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun NoteListScreen(
    viewModel: NoteViewModel,
    onNoteClick: (Int) -> Unit,
    onMapNoteClick: (Int) -> Unit,
    onAddNoteClick: (NoteTab) -> Unit,
    onSettingsClick: () -> Unit
) {
    val notes by viewModel.notes.collectAsState()
    val mapNotes by viewModel.mapNotes.collectAsState()
    val selectedMapId by viewModel.selectedMapId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val pinnedNotes by viewModel.pinnedNotes.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    
    val scope = rememberCoroutineScope()

    var isSearchExpanded by remember { mutableStateOf(false) }
    var isPillMenuExpanded by remember { mutableStateOf(false) }
    var showMapSelectionDialog by remember { mutableStateOf(false) }
    var noteToAddToMap by remember { mutableStateOf<Note?>(null) }

    val hasShownHints by viewModel.hasShownGestureHints.collectAsState()
    var showListHint by remember { mutableStateOf(false) } // superseded by VisualTutorialOverlay
    var showVisualTutorial by remember { mutableStateOf(false) }
    LaunchedEffect(hasShownHints) { if (!hasShownHints) { delay(800); showVisualTutorial = true } }

    LaunchedEffect(Unit) {
        // Give UI a moment to settle before loading notes
        kotlinx.coroutines.delay(300)
        viewModel.setLoaded()
    }

    val outerBg = MaterialTheme.colorScheme.background
    val cardBg = MaterialTheme.colorScheme.surfaceContainer
    val pillBg = MaterialTheme.colorScheme.surfaceContainerHighest
    Scaffold(containerColor = outerBg) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(outerBg)
        ) {
                        Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .imePadding()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.dotnotes_logo),
                        contentDescription = "DotNotes logo",
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "DotNotes",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.2).sp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    if (selectedTab == NoteTab.SEARCH) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            SearchAndMapsTab(
                                viewModel = viewModel,
                                mapNotes = mapNotes,
                                selectedMapId = selectedMapId,
                                searchQuery = searchQuery,
                                notes = notes,
                                isLoading = isLoading,
                                onNoteClick = onNoteClick,
                                onMapNoteClick = onMapNoteClick,
                                onAddToMap = { note -> noteToAddToMap = note; showMapSelectionDialog = true }
                            )
                        }
                    } else {
                        NoteListContent(
                            notes = notes,
                            isLoading = isLoading,
                            searchQuery = searchQuery,
                            viewModel = viewModel,
                            onNoteClick = onNoteClick,
                            onMapNoteClick = onMapNoteClick,
                            onAddToMap = { noteToAddToMap = it },
                            modifier = Modifier.fillMaxSize(),
                            showHint = showListHint,
                            onDismissHint = { showListHint = false }
                        )
                    }
                }
            }
            // Pill overlay — does not push header/list, same as other screens' floating but now correctly above
            val searchFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
            val listPillH by androidx.compose.animation.core.animateDpAsState(targetValue = if (isPillMenuExpanded) 320.dp else 56.dp, animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.85f, stiffness=300f), label="listH")
            val listPillCorner by androidx.compose.animation.core.animateDpAsState(targetValue = if (isPillMenuExpanded) 32.dp else 28.dp, animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.85f, stiffness=300f), label="listCorner")
            val listPillS = RoundedCornerShape(listPillCorner)
            androidx.compose.animation.AnimatedVisibility(
                visible = true,
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 12.dp).imePadding(),
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut()
            ) {
            Surface(shape = listPillS, color = pillBg, modifier = Modifier.fillMaxWidth().height(listPillH)
                    .pointerInput(Unit) {
                        var totalX = 0f
                        var totalY = 0f
                        detectDragGestures(
                            onDragEnd = {
                                if (isPillMenuExpanded && totalY > 80) isPillMenuExpanded = false
                                else if (totalY < -80) isPillMenuExpanded = true
                                else if (totalX > 100) {
                                    val cur = selectedTab.ordinal
                                    if (cur > 0) viewModel.setSelectedTab(NoteTab.entries[cur-1])
                                } else if (totalX < -100) {
                                    val cur = selectedTab.ordinal
                                    if (cur < NoteTab.entries.size-1) viewModel.setSelectedTab(NoteTab.entries[cur+1])
                                }
                                totalX = 0f; totalY = 0f
                            },
                            onDragCancel = { totalX = 0f; totalY = 0f },
                            onDrag = { change, dragAmount -> change.consume(); totalX += dragAmount.x; totalY += dragAmount.y }
                        )
                    }
                    .combinedClickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onLongClick = { if (selectedTab == NoteTab.MAPS) viewModel.createMapNote("New Map") else onAddNoteClick(selectedTab) },
                        onClick = { isPillMenuExpanded = !isPillMenuExpanded }
                    )) {
                    androidx.compose.animation.AnimatedContent(targetState = isPillMenuExpanded, label="listMenu") { exp ->
                        if (exp) {
                            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp).verticalScroll(rememberScrollState())) {
                                Box(modifier = Modifier.width(28.dp).height(3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)).align(Alignment.CenterHorizontally))
                                Spacer(modifier = Modifier.height(14.dp))
                                Text("Menu", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxWidth())
                                Text("${notes.size} notes • ${selectedTab.name.lowercase()} • ${if (selectedTag != null) "#"+selectedTag else "all tags"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 12.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f))
                                Spacer(modifier = Modifier.height(12.dp))
                                var showSort by remember { mutableStateOf(false) }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    val segItems = listOf(
                                        Triple(Icons.Default.Sort, "Sort • ${sortOrder.label}", { showSort = true }),
                                        Triple(if (viewMode == "grid") Icons.Default.ViewAgenda else Icons.Default.GridView, if (viewMode == "grid") "List view" else "Grid view", { viewModel.setViewMode(if (viewMode == "grid") "list" else "grid") }),
                                        Triple(Icons.Default.Settings, "Settings", { isPillMenuExpanded = false; onSettingsClick() })
                                    )
                                    segItems.forEachIndexed { idx, triple ->
                                        val icon = triple.first
                                        val label = triple.second
                                        val onClick = triple.third
                                        val shape = when {
                                            segItems.size <= 1 -> RoundedCornerShape(28.dp)
                                            idx == 0 -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
                                            idx == segItems.size - 1 -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
                                            else -> RoundedCornerShape(6.dp)
                                        }
                                        Card(
                                            shape = shape,
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                            modifier = Modifier.fillMaxWidth().clickable { onClick() }
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
                                                    Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                                                }
                                                Spacer(modifier = Modifier.width(14.dp))
                                                Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                                Icon(Icons.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f), modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                                Box {
                                    DropdownMenu(expanded = showSort, onDismissRequest = { showSort = false }) {
                                        SortOrder.entries.forEach { o -> DropdownMenuItem(text = { Text(o.label) }, onClick = { viewModel.setSortOrder(o); showSort = false }) }
                                    }
                                }
                                if (allTags.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Filter by tags", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                                    androidx.compose.foundation.layout.FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(selected = selectedTag == null, onClick = { viewModel.setSelectedTag(null) }, label = { Text("All") })
                                        allTags.forEach { tag -> FilterChip(selected = selectedTag == tag, onClick = { viewModel.setSelectedTag(if (selectedTag == tag) null else tag) }, label = { Text("#"+tag) }) }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)).padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                                    Text("Swipe pill to switch tabs • Long-press + to add", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.weight(1f).padding(start = 8.dp).clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { searchFocusRequester.requestFocus() }, contentAlignment = Alignment.CenterStart) {
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { viewModel.onSearchQueryChange(it) },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface, textAlign = androidx.compose.ui.text.style.TextAlign.Start),
                                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.onSurface),
                                        modifier = Modifier.fillMaxWidth().focusRequester(searchFocusRequester),
                                        decorationBox = { inner ->
                                            Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxWidth()) {
                                                if (searchQuery.isEmpty()) {
                                                    Text(text = if (selectedMapId != null) { mapNotes.find{it.id==selectedMapId}?.title ?: "Search"} else "Search", style=MaterialTheme.typography.titleMedium.copy(color=MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign=androidx.compose.ui.text.style.TextAlign.Start), maxLines=1)
                                                }
                                                inner()
                                            }
                                        }
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = pillBg,
                                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.9f)),
                                    modifier = Modifier.size(width = 72.dp, height = 44.dp).clickable { if (selectedTab == NoteTab.MAPS) viewModel.createMapNote("New Map") else onAddNoteClick(selectedTab) }
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMapSelectionDialog && noteToAddToMap != null) {
        MapNoteSelectionDialog(
            mapNotes = mapNotes,
            onMapSelected = { mapNote ->
                if (noteToAddToMap!!.id !in mapNote.linkedNoteIds) {
                    scope.launch {
                        viewModel.saveNote(
                            id = mapNote.id,
                            title = mapNote.title,
                            content = mapNote.content,
                            linkedNoteIds = mapNote.linkedNoteIds + noteToAddToMap!!.id
                        )
                    }
                }
                showMapSelectionDialog = false
                noteToAddToMap = null
            },
            onDismiss = {
                showMapSelectionDialog = false
                noteToAddToMap = null
            }
        )
    }

    if (showVisualTutorial) {
        com.folius.dotnotes.ui.components.VisualTutorialOverlay(
            onDismiss = { showVisualTutorial = false; viewModel.setHasShownGestureHints(true) },
            modifier = Modifier.fillMaxSize(),
            isPillExpanded = isPillMenuExpanded
        )
    }

    // Folder dialogs removed.
}


@Composable
fun NoteItem(note: Note, onClick: () -> Unit, onLongPress: () -> Unit, shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.medium) {
    val date = remember(note.timestamp) {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(note.timestamp))
    }
    val noteColorValue = note.color
    val baseBg = MaterialTheme.colorScheme.surfaceContainerLow
    val cardBg = remember(noteColorValue, baseBg) {
        if (noteColorValue != null) {
            val c = Color(noteColorValue)
            // 0.18 alpha tint blended over surface — keeps white text readable, still shows hue on homescreen
            c.copy(alpha = 0.22f).compositeOver(baseBg)
        } else baseBg
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress() }
                )
            },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = if (noteColorValue != null) BorderStroke(1.dp, Color(noteColorValue).copy(alpha = 0.35f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row {
            // Color indicator
            if (noteColorValue != null) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .clip(MaterialTheme.shapes.small)
                        .background(Color(noteColorValue.toLong() or 0xFF000000L))
                )
            }
        Column(
            modifier = Modifier.padding(16.dp).weight(1f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (note.title.isNotBlank()) note.title else "Untitled",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (note.isPinned) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                if (note.isMap) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.Map,
                        contentDescription = "Map Note",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            // Tags
            if (note.tags.isNotEmpty()) {
                Text(
                    text = note.tags.joinToString(" ") { "#$it" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (note.isChecklist) {
                val previewItems = note.checklist.take(3)
                Column {
                    previewItems.forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (item.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (note.checklist.size > 3) {
                        Text("...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                val truncatedContent = if (note.content.length > 100) {
                    note.content.take(100) + "..."
                } else {
                    note.content
                }
                Text(
                    text = truncatedContent,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        } // end Row
    }
}

@Composable
fun GridNoteItem(note: Note, onClick: () -> Unit, onLongPress: () -> Unit) {
    val date = remember(note.timestamp) { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(note.timestamp)) }
    val baseGridBg = MaterialTheme.colorScheme.surfaceContainer
    val gridBg = remember(note.color, baseGridBg) {
        if (note.color != null) Color(note.color).copy(alpha = 0.24f).compositeOver(baseGridBg) else baseGridBg
    }
    val gridBorder = if (note.color != null) BorderStroke(1.dp, Color(note.color).copy(alpha = 0.38f)) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongPress),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = gridBg),
        border = gridBorder,
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = if (note.title.isBlank()) "Untitled" else note.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (note.isPinned) { Spacer(Modifier.width(6.dp)); Icon(Icons.Default.PushPin, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) }
            }
            if (note.tags.isNotEmpty()) { Text(text = note.tags.joinToString(" ") { "#$it" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp)) }
            Spacer(Modifier.height(6.dp))
            if (note.isChecklist) {
                val done = note.checklist.count { it.isChecked }
                val total = note.checklist.size
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    note.checklist.take(8).forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (item.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            Spacer(Modifier.width(6.dp))
                            Text(item.text, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    if (total > 8) Text("…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (total > 0) Text("+ $done ticked", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.padding(top = 4.dp))
                }
            } else {
                val preview = if (note.content.length > 180) note.content.take(180) + "…" else note.content
                if (preview.isNotBlank()) Text(text = preview, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun MapNoteSelectionDialog(
    mapNotes: List<Note>,
    onMapSelected: (Note) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                                .clickable { onMapSelected(mapNote) }
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
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NoteListContent(
    notes: List<Note>,
    isLoading: Boolean,
    searchQuery: String,
    viewModel: NoteViewModel,
    onNoteClick: (Int) -> Unit,
    onMapNoteClick: (Int) -> Unit,
    onAddToMap: (Note) -> Unit,
    modifier: Modifier = Modifier,
    showHint: Boolean = false,
    onDismissHint: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val viewMode by viewModel.viewMode.collectAsState()
    Column(modifier = modifier.fillMaxSize()) {
        if (isLoading) {
            if (viewMode == "grid") {
                LazyVerticalStaggeredGrid(columns = StaggeredGridCells.Fixed(2), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp), verticalItemSpacing = 8.dp, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(10) { Box(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 140.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { items(10) { NoteItemSkeleton() } }
            }
        } else if (notes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (searchQuery.isEmpty()) "No notes here." else "No results found.", style = MaterialTheme.typography.bodyLarge)
            }
        } else if (viewMode == "grid") {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalItemSpacing = 8.dp,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    GridNoteItem(note = note, onClick = { if (note.isMap) onMapNoteClick(note.id) else onNoteClick(note.id) }, onLongPress = { onAddToMap(note) })
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                itemsIndexed(
                    items = notes,
                    key = { _, n -> n.id },
                    contentType = { _, _ -> "note" }
                ) { index, note ->
                    val density = LocalDensity.current
                    val midwayThreshold = with(density) { 120.dp.toPx() }
                    val dismissThreshold = with(density) { 250.dp.toPx() } // Lowered threshold for full action
                    
                    val decaySpec = exponentialDecay<Float>()
                    val state = remember {
                        AnchoredDraggableState<DragAnchors>(
                            initialValue = DragAnchors.Settled,
                            positionalThreshold = { distance: Float -> distance * 0.5f },
                            velocityThreshold = { with(density) { 100.dp.toPx() } },
                            snapAnimationSpec = tween(),
                            decayAnimationSpec = decaySpec
                        )
                    }

                    SideEffect {
                        state.updateAnchors(
                            DraggableAnchors {
                                DragAnchors.Settled at 0f
                                // Left swipe (EndToStart)
                                DragAnchors.RevealedEnd at -midwayThreshold
                                DragAnchors.DismissedEnd at -dismissThreshold
                                // Right swipe (StartToEnd)
                                DragAnchors.RevealedStart at midwayThreshold
                                DragAnchors.DismissedStart at dismissThreshold
                            }
                        )
                    }

                    // Haptic Detent: Trigger feel when snapping to midway or dismissal
                    LaunchedEffect(state.currentValue) {
                        if (state.currentValue != DragAnchors.Settled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }

                    // Trigger actions when dismissed
                    LaunchedEffect(state.currentValue) {
                        when (state.currentValue) {
                            DragAnchors.DismissedStart -> {
                                viewModel.trashNote(note)
                                Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show()
                                state.snapTo(DragAnchors.Settled)
                            }
                            DragAnchors.DismissedEnd -> {
                                viewModel.toggleNoteSecretStatus(note)
                                Toast.makeText(context, if (note.isSecret) "Note unlocked" else "Note locked", Toast.LENGTH_SHORT).show()
                                state.snapTo(DragAnchors.Settled)
                            }
                            else -> {}
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .anchoredDraggable(state, Orientation.Horizontal)
                            .clip(MaterialTheme.shapes.medium)
                    ) {
                        // Background (Actions)
                        val offset = if (state.anchors.size > 0) state.requireOffset() else 0f
                        val direction = if (offset > 0) DragAnchors.StartToEnd else DragAnchors.EndToStart
                        
                        val isRevealed = (abs(offset) >= midwayThreshold * 0.9f)
                        val isDismissing = (abs(offset) >= (midwayThreshold + dismissThreshold) / 2f)

                        val backgroundColor by animateColorAsState(
                            targetValue = when {
                                isDismissing && direction == DragAnchors.StartToEnd -> MaterialTheme.colorScheme.error
                                isDismissing && direction == DragAnchors.EndToStart -> MaterialTheme.colorScheme.tertiary
                                direction == DragAnchors.StartToEnd -> MaterialTheme.colorScheme.errorContainer.copy(alpha = if (isRevealed) 1f else 0.4f)
                                direction == DragAnchors.EndToStart -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = if (isRevealed) 1f else 0.4f)
                                else -> Color.Transparent
                            }
                        )

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(backgroundColor)
                                .clickable {
                                    val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
                                    if (state.currentValue == DragAnchors.RevealedEnd) {
                                        viewModel.toggleNoteSecretStatus(note)
                                        Toast.makeText(context, if (note.isSecret) "Note unlocked" else "Note locked", Toast.LENGTH_SHORT).show()
                                        scope.launch { state.snapTo(DragAnchors.Settled) }
                                    } else if (state.currentValue == DragAnchors.RevealedStart) {
                                        viewModel.trashNote(note)
                                        Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show()
                                        scope.launch { state.snapTo(DragAnchors.Settled) }
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            contentAlignment = if (offset > 0) Alignment.CenterStart else Alignment.CenterEnd
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = when {
                                        offset > 0 -> Icons.Default.Delete
                                        else -> if (note.isSecret) Icons.Default.LockOpen else Icons.Default.Lock
                                    },
                                    contentDescription = null,
                                    tint = if (isDismissing) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = if (isDismissing) 1.3f else 1.0f
                                        scaleY = if (isDismissing) 1.3f else 1.0f
                                    }
                                )
                                if (isRevealed) {
                                    Text(
                                        text = if (isDismissing) "Release to Confirm" else "Action Ready",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isDismissing) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Content (Note Item)
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(offset.roundToInt(), 0) }
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            NoteItem(
                                note = note, 
                                onClick = { 
                                    if (note.isMap) onMapNoteClick(note.id)
                                    else onNoteClick(note.id)
                                },
                                onLongPress = { 
                                    onAddToMap(note)
                                },
                                shape = segmentedShape(index, notes.size)
                            )
                        }
                    }
                }
            }
            
            // Gesture Hint Over Note List
            androidx.compose.animation.AnimatedVisibility(
                visible = showHint,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 150.dp),
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Filled.TouchApp, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Try swiping notes to Delete or Lock",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        IconButton(onClick = onDismissHint, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun NoteItemSkeleton() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(24.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.3f)
                    .height(16.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
            )
        }
    }
}

@Composable
fun SearchAndMapsTab(
    viewModel: NoteViewModel,
    mapNotes: List<Note>,
    selectedMapId: Int?,
    searchQuery: String,
    notes: List<Note>,
    isLoading: Boolean,
    onNoteClick: (Int) -> Unit,
    onMapNoteClick: (Int) -> Unit,
    onAddToMap: (Note) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
            .padding(16.dp)
    ) {
        NoteListContent(
            notes = notes,
            isLoading = isLoading,
            searchQuery = searchQuery,
            viewModel = viewModel,
            onNoteClick = onNoteClick,
            onMapNoteClick = onMapNoteClick,
            onAddToMap = onAddToMap,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Map Views", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedMapId == null,
                    onClick = { viewModel.setSelectedMap(null) },
                    label = { Text("All") }
                )
            }
            
            items(mapNotes, key = { it.id }) { note ->
                FilterChip(
                    selected = selectedMapId == note.id,
                    onClick = { viewModel.setSelectedMap(note.id) },
                    label = { Text(note.title) }
                )
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            placeholder = { Text("Search notes...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true
        )

        // Tag filter chips
        val allTags by viewModel.allTags.collectAsState()
        val selectedTag by viewModel.selectedTag.collectAsState()
        if (allTags.isNotEmpty()) {
            Text("Tags", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 4.dp, top = 8.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedTag == null,
                        onClick = { viewModel.setSelectedTag(null) },
                        label = { Text("All") },
                        leadingIcon = { Icon(Icons.Default.Label, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }
                items(allTags, key = { it }) { tag ->
                    FilterChip(
                        selected = selectedTag == tag,
                        onClick = { viewModel.setSelectedTag(if (selectedTag == tag) null else tag) },
                        label = { Text("#$tag") }
                    )
                }
            }
        }
        
        Text(
            text = "Notes are filtered by their links to the selected Map Note.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}
