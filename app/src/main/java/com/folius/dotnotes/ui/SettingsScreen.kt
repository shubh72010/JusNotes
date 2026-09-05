package com.folius.dotnotes.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue

private val GroupLargeCorner = 28.dp
private val GroupSmallCorner = 6.dp
private fun segmentedShape(index: Int, count: Int): androidx.compose.ui.graphics.Shape =
    when {
        count <= 1 -> RoundedCornerShape(GroupLargeCorner)
        index == 0 -> RoundedCornerShape(topStart = GroupLargeCorner, topEnd = GroupLargeCorner, bottomStart = GroupSmallCorner, bottomEnd = GroupSmallCorner)
        index == count - 1 -> RoundedCornerShape(topStart = GroupSmallCorner, topEnd = GroupSmallCorner, bottomStart = GroupLargeCorner, bottomEnd = GroupLargeCorner)
        else -> RoundedCornerShape(GroupSmallCorner)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NoteViewModel,
    onBack: () -> Unit,
    onSecretNotesClick: () -> Unit,
    onTrashClick: () -> Unit = {},
    onTutorialClick: () -> Unit = {}
) {
    val currentApiKey by viewModel.apiKey.collectAsState()
    val currentModelId by viewModel.modelId.collectAsState()
    val currentTheme by viewModel.theme.collectAsState()
    val animationsEnabledPref by viewModel.isAnimationsEnabled.collectAsState()
    val currentStorageUri by viewModel.storageUri.collectAsState()
    val deletedNotes by viewModel.deletedNotes.collectAsState()
    val secretNotes by viewModel.secretNotes.collectAsState()

    val context = LocalContext.current

    var apiKey by remember { mutableStateOf(currentApiKey ?: "") }
    var modelId by remember { mutableStateOf(currentModelId) }
    var animationsEnabled by remember { mutableStateOf(animationsEnabledPref) }
    var selectedTheme by remember { mutableStateOf(currentTheme) }
    var isPillMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(currentApiKey) { apiKey = currentApiKey ?: "" }
    LaunchedEffect(currentModelId) { modelId = currentModelId }
    LaunchedEffect(animationsEnabledPref) { animationsEnabled = animationsEnabledPref }
    LaunchedEffect(currentTheme) { selectedTheme = currentTheme }

    // reveal flags for inline editors
    var showAiFields by remember { mutableStateOf(false) }
    var showStorageDetails by remember { mutableStateOf(false) }

    val outerBg = MaterialTheme.colorScheme.background
    val cardBg = MaterialTheme.colorScheme.surfaceContainer
    val pillBg = MaterialTheme.colorScheme.surfaceContainerHighest

    val storagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            viewModel.setStorageUri(it.toString())
            try {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {}
        }
    }

    val importPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importNotes(uris) { count ->
                Toast.makeText(context, "Imported $count note${if (count != 1) "s" else ""}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(containerColor = outerBg) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(outerBg)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Card ────────────────────────────────
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = cardBg,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Header meta
                        Column {
                            Text(
                                "Settings",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.8).sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "${deletedNotes.size} in trash • ${secretNotes.size} secret • ${selectedTheme}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        // ── Vault / Manage ────────────
                        SectionLabel("Vault")
                        SegmentedGroup(
                            items = listOf(
                                SegmentRowData(Icons.Default.Delete, "Trash", "${deletedNotes.size} items • 30 days", onTrashClick),
                                SegmentRowData(Icons.Default.Lock, "Secret Notes", if (secretNotes.isEmpty()) "Locked with biometrics" else "${secretNotes.size} hidden", onSecretNotesClick),
                            )
                        )

                        // ── Appearance ────────────────
                        SectionLabel("Appearance")

                        // Theme picker card (single segmented card that holds FlowRow + switch)
                        Card(
                            shape = RoundedCornerShape(GroupLargeCorner),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            elevation = CardDefaults.cardElevation(0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text("Theme", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val themeOptions = listOf(
                                        ThemeData("System", Color(0xFF6750A4), Color(0xFFF3EDF7)),
                                        ThemeData("Material You", Color(0xFF4285F4), Color(0xFFFBBC05)),
                                        ThemeData("Light", Color(0xFF6650A4), Color.White),
                                        ThemeData("Dark", Color(0xFFD0BCFF), Color(0xFF1C1B1F))
                                    )
                                    themeOptions.forEach { data ->
                                        ThemeSquircleCard(
                                            name = data.name,
                                            primaryColor = data.primary,
                                            backgroundColor = data.background,
                                            isSelected = selectedTheme == data.name,
                                            onClick = {
                                                selectedTheme = data.name
                                                viewModel.saveSettings(apiKey, modelId, data.name, animationsEnabled)
                                            }
                                        )
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.14f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Animation, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Text("Text animations", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                    }
                                    Switch(
                                        checked = animationsEnabled,
                                        onCheckedChange = {
                                            animationsEnabled = it
                                            viewModel.saveSettings(apiKey, modelId, selectedTheme, it)
                                        }
                                    )
                                }
                            }
                        }

                        // ── Intelligence ──────────────
                        SectionLabel("Intelligence")
                        SegmentedGroup(
                            items = listOf(
                                SegmentRowData(
                                    Icons.Default.SmartToy, "AI Assistant",
                                    if (showAiFields) "Tap to hide" else if (apiKey.isBlank()) "Not configured" else modelId,
                                    { showAiFields = !showAiFields }
                                ),
                            )
                        )
                        if (showAiFields) {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(
                                        value = apiKey,
                                        onValueChange = { apiKey = it; viewModel.saveSettings(it, modelId, selectedTheme, animationsEnabled) },
                                        label = { Text("OpenRouter API Key") },
                                        placeholder = { Text("sk-or-v1-…") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = modelId,
                                        onValueChange = { modelId = it; viewModel.saveSettings(apiKey, it, selectedTheme, animationsEnabled) },
                                        label = { Text("Model ID") },
                                        placeholder = { Text("google/gemini-flash-1.5") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        "Stored locally via DataStore. Never leaves device except for AI calls.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                    )
                                }
                            }
                        }

                        // ── Storage ───────────────────
                        SectionLabel("Storage & Backup")
                        SegmentedGroup(
                            items = listOf(
                                SegmentRowData(
                                    Icons.Default.FolderOpen,
                                    if (currentStorageUri != null) "Backup folder" else "Set backup folder",
                                    if (currentStorageUri != null) "Tap to change" else "Choose directory",
                                    { storagePicker.launch(null) }
                                ),
                                SegmentRowData(Icons.Default.Backup, "Backup now", "Exports .json to chosen folder", {
                                    if (currentStorageUri == null) {
                                        Toast.makeText(context, "Set a backup folder first", Toast.LENGTH_SHORT).show()
                                        storagePicker.launch(null)
                                    } else {
                                        viewModel.backupNotes(
                                            onSuccess = { Toast.makeText(context, "Backup successful", Toast.LENGTH_SHORT).show() },
                                            onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                        )
                                    }
                                }),
                                SegmentRowData(Icons.Default.FileOpen, "Import notes", ".txt  .md  .json", {
                                    importPicker.launch(arrayOf("text/plain", "text/markdown", "application/json", "application/octet-stream"))
                                }),
                            )
                        )
                        if (currentStorageUri != null) {
                            Text(
                                currentStorageUri!!.take(72),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                maxLines = 1
                            )
                        }

                        // ── Help ────────────────────────
                        SectionLabel("Help")
                        SegmentedGroup(
                            items = listOf(
                                SegmentRowData(Icons.Default.School, "View tutorial", "Overlay on your real screen", onTutorialClick),
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Swipe the pill to go back • Tap to expand",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Pill ──────────────────────────────
                val pillH by animateDpAsState(
                    targetValue = if (isPillMenuExpanded) 220.dp else 56.dp,
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f),
                    label = "pillH"
                )
                val pillCorner by animateDpAsState(
                    targetValue = if (isPillMenuExpanded) 32.dp else 28.dp,
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f),
                    label = "pillCorner"
                )
                val pillShape = RoundedCornerShape(pillCorner)
                Surface(
                    shape = pillShape,
                    color = pillBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(pillH)
                        .pointerInput(Unit) {
                            var dX = 0f
                            var dY = 0f
                            detectDragGestures(
                                onDragEnd = {
                                    if (isPillMenuExpanded && dY > 80) isPillMenuExpanded = false
                                    else if (!isPillMenuExpanded && dY < -80) isPillMenuExpanded = true
                                    else if (!isPillMenuExpanded && dX.absoluteValue > 80) onBack()
                                    dX = 0f; dY = 0f
                                },
                                onDragCancel = { dX = 0f; dY = 0f },
                                onDrag = { change, amt -> change.consume(); dX += amt.x; dY += amt.y }
                            )
                        }
                        .combinedClickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                            onLongClick = {},
                            onClick = {
                                if (isPillMenuExpanded) isPillMenuExpanded = false else onBack()
                            }
                        )
                ) {
                    AnimatedContent(targetState = isPillMenuExpanded, label = "settingsPill") { exp ->
                        if (exp) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(modifier = Modifier.width(28.dp).height(3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)))
                                Spacer(modifier = Modifier.height(4.dp))
                                SegmentedGroup(
                                    items = listOf(
                                        SegmentRowData(Icons.Default.Delete, "Open Trash", "${deletedNotes.size} items", { isPillMenuExpanded = false; onTrashClick() }),
                                        SegmentRowData(Icons.Default.Lock, "Secret Notes", "Biometric", { isPillMenuExpanded = false; onSecretNotesClick() }),
                                    )
                                )
                                TextButton(onClick = { isPillMenuExpanded = false }) { Text("Close") }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    "settings",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
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
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.6.sp, fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

private data class SegmentRowData(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)

@Composable
private fun SegmentedGroup(items: List<SegmentRowData>) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
        items.forEachIndexed { idx, row ->
            Card(
                shape = segmentedShape(idx, items.size),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth().clickable { row.onClick() }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(row.icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(row.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
                        if (row.subtitle.isNotBlank()) {
                            Text(row.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1)
                        }
                    }
                    Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun ThemeSquircleCard(
    name: String,
    primaryColor: Color,
    backgroundColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(74.dp)
            .clickable(onClick = onClick)
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(MaterialTheme.shapes.large)
                .background(backgroundColor)
                .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = if (isSelected) primaryColor else MaterialTheme.colorScheme.outlineVariant,
                    shape = MaterialTheme.shapes.large
                ),
            contentAlignment = Alignment.Center
        ) {
            if (name == "Material You") {
                Column(modifier = Modifier.size(28.dp)) {
                    Row(modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(topStart = 8.dp)).background(Color(0xFF4285F4)))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(topEnd = 8.dp)).background(Color(0xFFEA4335)))
                    }
                    Row(modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(bottomStart = 8.dp)).background(Color(0xFFFBBC05)))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(bottomEnd = 8.dp)).background(Color(0xFF34A853)))
                    }
                }
            } else {
                Box(
                    modifier = Modifier.size(26.dp).clip(MaterialTheme.shapes.small).background(primaryColor)
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if (backgroundColor == Color.White) primaryColor else Color.White,
                    modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            maxLines = 1,
            color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface
        )
    }
}

data class ThemeData(val name: String, val primary: Color, val background: Color)
