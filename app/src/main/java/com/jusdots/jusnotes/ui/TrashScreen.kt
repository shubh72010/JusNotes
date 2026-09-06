package com.jusdots.jusnotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jusdots.jusnotes.data.Note
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: NoteViewModel,
    onBack: () -> Unit
) {
    val deletedNotes by viewModel.deletedNotes.collectAsState()
    var isPillMenuExpanded by remember { mutableStateOf(false) }
    var showEmptyTrashDialog by remember { mutableStateOf(false) }


    val outerBg = MaterialTheme.colorScheme.background
    val cardBg = MaterialTheme.colorScheme.surfaceContainer
    val pillBg = MaterialTheme.colorScheme.surfaceContainerHighest
    Scaffold(containerColor = outerBg) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding).background(outerBg).padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(shape = RoundedCornerShape(32.dp), color = cardBg, modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (deletedNotes.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(48.dp), tint = Color.White.copy(alpha = 0.6f))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Trash is empty", style = MaterialTheme.typography.bodyLarge.copy(color = Color.White))
                                    Text("Deleted notes are kept for 30 days", style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.6f)))
                                }
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize().padding(20.dp), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                item { Text("${deletedNotes.size} item${if (deletedNotes.size != 1) "s" else ""} in trash", style = MaterialTheme.typography.labelMedium.copy(color = Color.White.copy(alpha = 0.6f))) }
                                items(deletedNotes, key = { it.id }) { note ->
                                    TrashNoteItem(note = note, onRestore = { viewModel.restoreNote(note) }, onPermanentDelete = { viewModel.permanentlyDeleteNote(note) })
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))

                val trashH by androidx.compose.animation.core.animateDpAsState(targetValue = if (isPillMenuExpanded) 200.dp else 56.dp, animationSpec = androidx.compose.animation.core.spring(dampingRatio=0.85f, stiffness=300f), label="trashH")
                val trashCorner by androidx.compose.animation.core.animateDpAsState(targetValue = if (isPillMenuExpanded) 32.dp else 28.dp, animationSpec = androidx.compose.animation.core.spring(dampingRatio=0.85f, stiffness=300f), label="trashCorner")
                val trashS = RoundedCornerShape(trashCorner)
                Surface(shape = trashS, color = pillBg, modifier = Modifier.fillMaxWidth().height(trashH)
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
                    .combinedClickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onLongClick={}, onClick={ onBack() })) {
                    androidx.compose.animation.AnimatedContent(targetState = isPillMenuExpanded, label="trashMenu") { exp ->
                        if (exp) {
                            Column(modifier=Modifier.fillMaxSize().padding(16.dp), horizontalAlignment=Alignment.CenterHorizontally) {
                                Box(modifier=Modifier.width(32.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha=0.3f)))
                                Spacer(modifier=Modifier.height(12.dp))
                                if (deletedNotes.isNotEmpty()) { Button(onClick={ isPillMenuExpanded=false; showEmptyTrashDialog=true }, modifier=Modifier.fillMaxWidth(), colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.error)) { Text("Empty Trash") } }
                                OutlinedButton(onClick={ isPillMenuExpanded=false; onBack() }, modifier=Modifier.fillMaxWidth()) { Text("Back", color=Color.White) }
                                TextButton(onClick={isPillMenuExpanded=false}) { Text("Close", color=Color.White) }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("trash", style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface, textAlign = androidx.compose.ui.text.style.TextAlign.Center))
                            }
                        }
                    }
                }
            }
        }
    }


    if (showEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashDialog = false },
            title = { Text("Empty Trash") },
            text = { Text("Permanently delete all ${deletedNotes.size} items? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.emptyTrash()
                        showEmptyTrashDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TrashNoteItem(
    note: Note,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    val deletedDate = remember(note.deletedTimestamp) {
        note.deletedTimestamp?.let {
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it))
        } ?: "Unknown"
    }

    val daysRemaining = remember(note.deletedTimestamp) {
        note.deletedTimestamp?.let {
            val thirtyDays = 30L * 24 * 60 * 60 * 1000
            val remaining = (it + thirtyDays - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)
            maxOf(0, remaining).toInt()
        } ?: 30
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = note.title.ifBlank { "Untitled" },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            val preview = if (note.isChecklist) {
                note.checklist.joinToString(", ") { it.text }.take(80)
            } else {
                note.content.take(80)
            }
            if (preview.isNotBlank()) {
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Deleted $deletedDate · $daysRemaining days left",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPermanentDelete) {
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = "Delete Permanently",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onRestore) {
                    Icon(
                        Icons.Default.RestoreFromTrash,
                        contentDescription = "Restore",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
