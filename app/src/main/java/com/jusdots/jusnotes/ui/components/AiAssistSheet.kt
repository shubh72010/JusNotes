package com.jusdots.jusnotes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.jusdots.jusnotes.ui.NoteViewModel
import kotlinx.coroutines.launch

private val GroupLarge = 28.dp
private val GroupSmall = 6.dp
private fun segShape(i: Int, n: Int) = when {
    n <= 1 -> RoundedCornerShape(GroupLarge)
    i == 0 -> RoundedCornerShape(topStart = GroupLarge, topEnd = GroupLarge, bottomStart = GroupSmall, bottomEnd = GroupSmall)
    i == n - 1 -> RoundedCornerShape(topStart = GroupSmall, topEnd = GroupSmall, bottomStart = GroupLarge, bottomEnd = GroupLarge)
    else -> RoundedCornerShape(GroupSmall)
}

enum class AiGroup { POLISH, TONE, STRUCTURE, ASK }

data class AiActionMeta(
    val id: String,
    val label: String,
    val desc: String,
    val icon: ImageVector,
    val group: AiGroup
)

val aiActions = listOf(
    AiActionMeta("Summary", "Summary", "Condense to key points", Icons.Default.Summarize, AiGroup.POLISH),
    AiActionMeta("Grammar & Clarity", "Grammar", "Fix grammar & clarity", Icons.Default.Spellcheck, AiGroup.POLISH),
    AiActionMeta("Formal Tone", "Formal", "Make it formal", Icons.Default.BusinessCenter, AiGroup.TONE),
    AiActionMeta("Casual Tone", "Casual", "Make it casual", Icons.Default.ChatBubble, AiGroup.TONE),
    AiActionMeta("Bullets to Prose", "Bullets → Prose", "List to paragraph", Icons.Default.Article, AiGroup.STRUCTURE),
    AiActionMeta("Prose to Bullets", "Prose → Bullets", "Paragraph to list", Icons.Default.FormatListBulleted, AiGroup.STRUCTURE),
    AiActionMeta("Q&A", "Ask", "Ask about this note", Icons.Default.HelpOutline, AiGroup.ASK),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistSheet(
    viewModel: NoteViewModel,
    content: String,
    onInsert: (String) -> Unit,
    onReplace: (String) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiKey by viewModel.apiKey.collectAsState()
    val modelId by viewModel.modelId.collectAsState()
    val hasKey = !apiKey.isNullOrBlank()
    var selected by remember { mutableStateOf(aiActions.first()) }
    var query by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    fun run() {
        if (!hasKey) {
            Toast.makeText(context, "Set API key in Settings → Intelligence", Toast.LENGTH_LONG).show()
            return
        }
        if (content.isBlank() && selected.id != "Q&A") {
            Toast.makeText(context, "Note is empty", Toast.LENGTH_SHORT).show()
            return
        }
        if (selected.id == "Q&A" && query.isBlank()) {
            Toast.makeText(context, "Ask something", Toast.LENGTH_SHORT).show()
            return
        }
        isLoading = true
        response = if (selected.id == "Q&A") "Thinking..." else "Processing..."
        val payload = if (selected.id == "Q&A") "Context:\n$content\n\nQuestion: $query" else content
        viewModel.processAiAction(selected.id, payload) { res ->
            response = res ?: "Error: No response"
            isLoading = false
        }
    }

    val canAct = !isLoading && response.isNotBlank() && !response.startsWith("Error") && !response.startsWith("Processing") && !response.startsWith("Thinking")

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), containerColor = MaterialTheme.colorScheme.surfaceContainer, dragHandle = { Box(Modifier.width(28.dp).height(3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))) }) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header — matches Settings header style
            Column {
                Text("AI Assist", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp), color = MaterialTheme.colorScheme.onSurface)
                Text(
                    if (hasKey) "$modelId • runs on your key, never stored" else "No API key — set in Settings → Intelligence",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hasKey) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.error
                )
            }
            if (!hasKey) {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VpnKeyOff, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(12.dp))
                        Text("Add your OpenRouter key to run AI. Key stays on-device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                    }
                }
            }

            // Segmented groups — 28/6 system
            AiGroupSection("Polish", aiActions.filter { it.group == AiGroup.POLISH }, selected, { selected = it })
            AiGroupSection("Rewrite", aiActions.filter { it.group == AiGroup.TONE }, selected, { selected = it })
            AiGroupSection("Structure", aiActions.filter { it.group == AiGroup.STRUCTURE }, selected, { selected = it })

            // Ask — separate card with input
            Card(shape = RoundedCornerShape(GroupLarge), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.HelpOutline, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Ask about this note", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            Text("Uses note as context", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                        RadioButton(selected = selected.id == "Q&A", onClick = { selected = aiActions.first { it.id == "Q&A" } })
                    }
                    if (selected.id == "Q&A") {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("e.g. Summarize as 3 bullets") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                            minLines = 2,
                            trailingIcon = {
                                IconButton(onClick = { run() }, enabled = !isLoading) {
                                    Icon(Icons.Default.Send, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                    }
                }
            }

            // Run button — only for non-Ask
            if (selected.id != "Q&A") {
                Button(onClick = { run() }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading && hasKey && content.isNotBlank()) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Icon(selected.icon, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isLoading) "Running..." else "Run ${selected.label}")
                }
            }

            // Response preview — surfaceContainerHigh
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp)) {
                Column(Modifier.padding(16.dp).fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Preview", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.weight(1f))
                        if (response.isNotBlank() && !isLoading) {
                            IconButton(onClick = {
                                val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                cm.setPrimaryClip(android.content.ClipData.newPlainText("AI", response))
                                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                            }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    if (isLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                        Spacer(Modifier.height(12.dp))
                    }
                    Text(
                        text = if (response.isBlank()) "Pick an action above — result shows here. You can copy, append, or replace." else response,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (response.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Append / Replace — segmented
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val actions = listOf(
                    Triple(Icons.Default.Add, "Append to note", { if (canAct) { onInsert("\n\n--- AI ${selected.label} ---\n$response"); onDismiss(); Toast.makeText(context, "Appended", Toast.LENGTH_SHORT).show() } }),
                    Triple(Icons.Default.ContentCopy, "Replace note", { if (canAct) { onReplace(response); onDismiss(); Toast.makeText(context, "Replaced", Toast.LENGTH_SHORT).show() } }),
                )
                actions.forEachIndexed { i, (icon, label, onClick) ->
                    val shape = segShape(i, actions.size)
                    Card(shape = shape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth().clickable(enabled = canAct) { onClick() }) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (canAct) 0.85f else 0.4f)), contentAlignment = Alignment.Center) {
                                Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = if (canAct) 1f else 0.4f))
                            }
                            Spacer(Modifier.width(14.dp))
                            Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (canAct) 1f else 0.4f), modifier = Modifier.weight(1f))
                            Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f), modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Text("Append keeps your note + adds AI. Replace overwrites — undo with Pill swipe.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.padding(top = 6.dp, start = 4.dp))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AiGroupSection(title: String, items: List<AiActionMeta>, selected: AiActionMeta, onPick: (AiActionMeta) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.6.sp, fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f), modifier = Modifier.padding(start = 4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items.forEachIndexed { i, meta ->
                val isSel = selected.id == meta.id
                Card(
                    shape = segShape(i, items.size),
                    colors = CardDefaults.cardColors(containerColor = if (isSel) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f) else MaterialTheme.colorScheme.surfaceContainerLow),
                    border = if (isSel) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) else null,
                    modifier = Modifier.fillMaxWidth().clickable { onPick(meta) }
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
                            Icon(meta.icon, null, tint = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(meta.label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
                            Text(meta.desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                        RadioButton(selected = isSel, onClick = { onPick(meta) })
                    }
                }
            }
        }
    }
}
