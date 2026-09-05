package com.folius.dotnotes.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Map
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.folius.dotnotes.data.MapItem
import com.folius.dotnotes.data.Note
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteMapScreen(
    noteId: Int,
    viewModel: NoteViewModel,
    mapItems: List<com.folius.dotnotes.data.MapItem>,
    onMapItemsChange: (List<com.folius.dotnotes.data.MapItem>) -> Unit,
    onNavigateToNote: (Int) -> Unit,
    onBack: () -> Unit
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val allNotes by viewModel.allNonDeletedNotes.collectAsState(initial = emptyList())
    // Find the current "Map Note"
    val mapNote = allNotes.find { it.id == noteId }

    if (mapNote == null) {
        // Handle loading or error
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val scope = rememberCoroutineScope()
    
    // Local state for map items to allow smooth dragging before save
    var localMapItems by remember { mutableStateOf(mapItems) }
    
    // Sync local state when external items change (added/removed)
    LaunchedEffect(mapItems) {
        val localIds = localMapItems.map { it.id }.toSet()
        val externalIds = mapItems.map { it.id }.toSet()
        if (localIds != externalIds) {
            localMapItems = mapItems
        }
    }
    
    // Dialog state
    var showAddNoteDialog by remember { mutableStateOf(false) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 3f)
        offset += panChange
    }

    var isPillMenuExpanded by remember { mutableStateOf(false) }


    val outerBg = MaterialTheme.colorScheme.background
    val cardBg = MaterialTheme.colorScheme.surfaceContainer
    val pillBg = MaterialTheme.colorScheme.surfaceContainerHighest
    Scaffold(containerColor = outerBg) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding).background(outerBg).padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Map canvas inside rounded card
                Surface(shape = RoundedCornerShape(32.dp), color = cardBg, modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(32.dp))) {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize().background(cardBg).pointerInput(Unit) {
                            detectTapGestures(onTap = { focusManager.clearFocus(); isPillMenuExpanded = false })
                        }
                    ) {
                        val canvasWidth = constraints.maxWidth.toFloat()
                        val canvasHeight = constraints.maxHeight.toFloat()
                        val gridColor = Color.White.copy(alpha = 0.06f)
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val gridSize = 40.dp.toPx()
                            val scaledGrid = gridSize * scale
                            val startX = (offset.x % scaledGrid)
                            val startY = (offset.y % scaledGrid)
                            var x = startX - scaledGrid
                            while (x < size.width + scaledGrid) {
                                drawLine(gridColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1.dp.toPx())
                                x += scaledGrid
                            }
                            var y = startY - scaledGrid
                            while (y < size.height + scaledGrid) {
                                drawLine(gridColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1.dp.toPx())
                                y += scaledGrid
                            }
                        }
                        Box(
                            modifier = Modifier.fillMaxSize().transformable(transformableState).graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
                        ) {
                            val density = LocalDensity.current
                            val connectionColor = Color.White.copy(alpha = 0.18f)
                            val cardWidthPx = remember(density) { with(density) { 200.dp.toPx() } }
                            val cardHeightPx = remember(density) { with(density) { 100.dp.toPx() } }
                            val connections = remember(localMapItems, allNotes, cardWidthPx, cardHeightPx) {
                                val list = mutableListOf<Pair<Offset, Offset>>()
                                localMapItems.forEach { sourceItem ->
                                    val sourceNote = allNotes.find { it.id == sourceItem.noteId }
                                    sourceNote?.linkedNoteIds?.forEach { targetNoteId ->
                                        val targetItem = localMapItems.find { it.noteId == targetNoteId }
                                        if (targetItem != null) {
                                            val start = Offset(sourceItem.x + cardWidthPx / 2, sourceItem.y + cardHeightPx / 2)
                                            val end = Offset(targetItem.x + cardWidthPx / 2, targetItem.y + cardHeightPx / 2)
                                            list.add(start to end)
                                        }
                                    }
                                }
                                list
                            }
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                connections.forEach { (start, end) -> drawLine(connectionColor, start = start, end = end, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round) }
                            }
                            localMapItems.forEach { item ->
                                val linkedNote = allNotes.find { it.id == item.noteId }
                                if (linkedNote != null) {
                                    MapNoteCard(item = item, note = linkedNote, scale = scale, onPositionCommitted = { newX, newY -> val updated = localMapItems.map { if (it.id == item.id) it.copy(x = newX, y = newY) else it }; localMapItems = updated; onMapItemsChange(updated) }, onClick = { onNavigateToNote(linkedNote.id) }, onLongClick = { val updated = localMapItems.filter { it.id != item.id }; localMapItems = updated; onMapItemsChange(updated) })
                                }
                            }
                        }
                        if (localMapItems.isEmpty()) {
                            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Map, null, modifier = Modifier.size(64.dp), tint = Color.White.copy(alpha = 0.15f))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Long-press the pill to add notes", style = MaterialTheme.typography.bodyLarge.copy(color = Color.White.copy(alpha = 0.4f)))
                            }
                        }
                        if (showAddNoteDialog) {
                            AddNoteToMapDialog(
                                allNotes = allNotes.filter { note -> !note.isMap && !note.isDeleted && note.id != noteId && localMapItems.none { item -> item.noteId == note.id } },
                                onDismiss = { showAddNoteDialog = false },
                                onNoteSelected = { selectedNote ->
                                    val centerX = (canvasWidth / 2 - offset.x) / scale
                                    val centerY = (canvasHeight / 2 - offset.y) / scale
                                    val newItem = MapItem(noteId = selectedNote.id, x = centerX, y = centerY)
                                    val newItems = localMapItems + newItem; localMapItems = newItems; onMapItemsChange(newItems); showAddNoteDialog = false
                                },
                                onCreateNewNote = {
                                    scope.launch {
                                        val newId = viewModel.saveNote(id = null, title = "New Note", content = "")
                                        if (newId != -1) {
                                            val centerX = (canvasWidth / 2 - offset.x) / scale
                                            val centerY = (canvasHeight / 2 - offset.y) / scale
                                            val newItem = MapItem(noteId = newId, x = centerX, y = centerY)
                                            val newItems = localMapItems + newItem; localMapItems = newItems; onMapItemsChange(newItems)
                                        }
                                        showAddNoteDialog = false
                                    }
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))

                var localTitle by remember(mapNote.id) { mutableStateOf(mapNote.title) }
                val mapPH by androidx.compose.animation.core.animateDpAsState(targetValue = if (isPillMenuExpanded) 200.dp else 56.dp, animationSpec = androidx.compose.animation.core.spring(dampingRatio=0.85f, stiffness=300f), label="mapH")
                val mapCorner by androidx.compose.animation.core.animateDpAsState(targetValue = if (isPillMenuExpanded) 32.dp else 28.dp, animationSpec = androidx.compose.animation.core.spring(dampingRatio=0.85f, stiffness=300f), label="mapCorner")
                val mapPS = RoundedCornerShape(mapCorner)
                Surface(shape = mapPS, color = pillBg, modifier = Modifier.fillMaxWidth().height(mapPH)
                    .pointerInput(Unit) {
                        var dX = 0f
                        var dY = 0f
                        detectDragGestures(
                            onDragEnd = {
                                if (isPillMenuExpanded && dY > 80) isPillMenuExpanded = false
                                else if (dY < -80) isPillMenuExpanded = true
                                else if (!isPillMenuExpanded && kotlin.math.abs(dX)>80) onBack()
                                dX = 0f; dY = 0f
                            },
                            onDragCancel = { dX = 0f; dY = 0f },
                            onDrag = { change, amt ->
                                change.consume()
                                dX += amt.x
                                dY += amt.y
                            }
                        )
                    }
                    .combinedClickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onLongClick={ showAddNoteDialog = true }, onClick={ isPillMenuExpanded = !isPillMenuExpanded }) ) {
                    androidx.compose.animation.AnimatedContent(targetState = isPillMenuExpanded, label="mapMenu") { exp ->
                        if (exp) {
                            Column(modifier=Modifier.fillMaxSize().padding(16.dp), horizontalAlignment=Alignment.CenterHorizontally) {
                                Box(modifier=Modifier.width(32.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha=0.3f)))
                                Spacer(modifier=Modifier.height(12.dp))
                                Button(onClick={ isPillMenuExpanded=false; showAddNoteDialog=true }, modifier=Modifier.fillMaxWidth()) { Text("Add Note") }
                                OutlinedButton(onClick={ isPillMenuExpanded=false; onBack() }, modifier=Modifier.fillMaxWidth()) { Text("Back", color=Color.White) }
                                TextButton(onClick={isPillMenuExpanded=false}) { Text("Close", color=Color.White) }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                                androidx.compose.foundation.text.BasicTextField(
                                    value = localTitle,
                                    onValueChange = { localTitle = it; viewModel.updateNoteTitle(noteId, it) },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.onSurface),
                                    modifier = Modifier.fillMaxWidth(),
                                    decorationBox = { inner ->
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                                            if (localTitle.isEmpty()) Text("map", style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = androidx.compose.ui.text.style.TextAlign.Center))
                                            inner()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MapNoteCard(
    item: MapItem,
    note: Note,
    scale: Float,
    onPositionCommitted: (Float, Float) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var posX by remember { mutableFloatStateOf(item.x) }
    var posY by remember { mutableFloatStateOf(item.y) }
    val updatedScale = rememberUpdatedState(scale)
    
    val dragScale by animateFloatAsState(
        targetValue = if (isDragging) 1.05f else 1f,
        label = "dragScale"
    )
    
    Card(
        modifier = Modifier
            .offset { IntOffset(posX.roundToInt(), posY.roundToInt()) }
            .width(200.dp)
            .heightIn(min = 100.dp)
            .pointerInput(item.id, "tap") {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
            .pointerInput(item.id, "drag") {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { 
                        isDragging = false
                        onPositionCommitted(posX, posY)
                    },
                    onDragCancel = { 
                        isDragging = false
                        onPositionCommitted(posX, posY)
                    }
                ) { change, dragAmount ->
                    change.consume()
                    posX += dragAmount.x / updatedScale.value
                    posY += dragAmount.y / updatedScale.value
                }
            }
            .graphicsLayer {
                scaleX = dragScale
                scaleY = dragScale
            },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = note.color?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = note.title.ifBlank { "Untitled" },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AddNoteToMapDialog(
    allNotes: List<Note>,
    onDismiss: () -> Unit,
    onNoteSelected: (Note) -> Unit,
    onCreateNewNote: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Add Note to Map",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // "Create New" Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCreateNewNote() }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), MaterialTheme.shapes.extraSmall),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(8.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "Create New Note",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                HorizontalDivider(modifier = Modifier.alpha(0.5f))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(allNotes) { note ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNoteSelected(note) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Color Indicator
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .background(note.color?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), MaterialTheme.shapes.extraSmall)
                            )
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = note.title.ifBlank { "Untitled" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (note.content.isNotBlank()) {
                                    Text(
                                        text = note.content,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        HorizontalDivider(modifier = Modifier.alpha(0.5f))
                    }
                }
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}
