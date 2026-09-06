package com.jusdots.jusnotes.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jusdots.jusnotes.data.Note
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Declarative model ──

enum class TutorialStepId { WELCOME, PILL, NOTES, MAPS_SEARCH }
enum class Spotlight { PILL, NOTES, NONE }
enum class RequiredAction { NONE, TAP_PILL, SWIPE_NOTE }

data class TutorialStep(
    val id: TutorialStepId,
    val title: String,
    val desc: String,
    val icon: ImageVector,
    val spotlight: Spotlight,
    val requiresAction: RequiredAction = RequiredAction.NONE
)

// Single source of truth — used by overlay + standalone pager
val tutorialSteps = listOf(
    TutorialStep(
        TutorialStepId.WELCOME,
        "Meet JusNotes",
        "Two things: note cards that hold everything, and the Pill that moves you.",
        Icons.Default.Hub,
        Spotlight.NONE,
        RequiredAction.NONE
    ),
    TutorialStep(
        TutorialStepId.PILL,
        "Your notes, one swipe away",
        "Tap to expand • swipe to switch • drag up/down • long-press + to add.",
        Icons.Default.TouchApp,
        Spotlight.PILL,
        RequiredAction.TAP_PILL
    ),
    TutorialStep(
        TutorialStepId.NOTES,
        "Cards that work like you do",
        "Swipe a card → trash or lock. Pin keeps it on top. Try it on the preview.",
        Icons.Default.Swipe,
        Spotlight.NOTES,
        RequiredAction.SWIPE_NOTE
    ),
    TutorialStep(
        TutorialStepId.MAPS_SEARCH,
        "Find anything fast",
        "Maps group related notes. Search, tags and filters stay in the Pill.",
        Icons.Default.Search,
        Spotlight.NONE,
        RequiredAction.NONE
    )
)

// Preview notes — never touch DB, negative IDs guarantee no collision
val tutorialPreviewNotes = listOf(
    Note(
        id = -100,
        title = "Swipe me",
        content = "Swipe right → trash, left → lock. This is a preview — your notes stay safe.",
        timestamp = System.currentTimeMillis(),
        tags = listOf("try-it"),
        isPinned = true,
        color = 0xFF6750A4.toInt()
    ),
    Note(
        id = -101,
        title = "Pin & grid",
        content = "Pinned stays on top. Toggle list/grid from the Pill.",
        timestamp = System.currentTimeMillis() - 1000,
        tags = listOf("tips"),
        color = null
    )
)

@Composable
fun VisualTutorialOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isPillExpanded: Boolean = false,
    pillBounds: Rect? = null,
    notesBounds: Rect? = null,
    isAnimationsEnabled: Boolean = true,
    onStepSwiped: (() -> Unit)? = null
) {
    var idx by remember { mutableIntStateOf(0) }
    val step = tutorialSteps[idx]
    val isLast = idx == tutorialSteps.lastIndex
    val isFirst = idx == 0
    val density = LocalDensity.current
    val springSpec = spring<Float>(dampingRatio = 0.85f, stiffness = 300f)
    val tweenSpec = tween<Float>(durationMillis = 0)

    // Forced interaction: pill step auto-advances when pill expands
    LaunchedEffect(idx, isPillExpanded) {
        if (step.requiresAction == RequiredAction.TAP_PILL && step.spotlight == Spotlight.PILL) {
            if (idx == 1 && isPillExpanded) { delay(300); if (idx == 1) idx++ }
        }
    }

    val tappable = step.requiresAction != RequiredAction.TAP_PILL && step.requiresAction != RequiredAction.SWIPE_NOTE
    // SWIPE_NOTE is handled via preview swipe, not overlay click. TAP_PILL passes through.

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .then(if (tappable) Modifier.clickable { if (isLast) onDismiss() else idx++ } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        val maxW = constraints.maxWidth.toFloat()
        val maxH = constraints.maxHeight.toFloat()

        // Measured bounds or fallback
        val pillRect: Rect? = pillBounds ?: run {
            val pw = maxW - with(density) { 32.dp.toPx() }
            val ph = with(density) { 56.dp.toPx() }
            val px = (maxW - pw) / 2f
            val py = maxH - with(density) { 12.dp.toPx() } - ph - with(density) { 8.dp.toPx() }
            Rect(Offset(px, py), Size(pw, ph))
        }
        val notesRect: Rect? = notesBounds ?: run {
            val ny = with(density) { 88.dp.toPx() }
            val nh = with(density) { 170.dp.toPx() }
            Rect(Offset(with(density) { 12.dp.toPx() }, ny), Size(maxW - with(density) { 24.dp.toPx() }, nh))
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(Color.Black.copy(alpha = 0.64f))
            when (step.spotlight) {
                Spotlight.PILL -> pillRect?.let {
                    drawRoundRect(Color.Transparent, it.topLeft, it.size, CornerRadius(28.dp.toPx(), 28.dp.toPx()), blendMode = BlendMode.Clear)
                }
                Spotlight.NOTES -> notesRect?.let {
                    drawRoundRect(Color.Transparent, it.topLeft, it.size, CornerRadius(16.dp.toPx(), 16.dp.toPx()), blendMode = BlendMode.Clear)
                }
                Spotlight.NONE -> {}
            }
            // highlight stroke
            when (step.spotlight) {
                Spotlight.PILL -> pillRect?.let {
                    drawRoundRect(Color.White.copy(alpha = 0.35f), it.topLeft, it.size, CornerRadius(28.dp.toPx(), 28.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                }
                Spotlight.NOTES -> notesRect?.let {
                    drawRoundRect(Color.White.copy(alpha = 0.25f), it.topLeft, it.size, CornerRadius(16.dp.toPx(), 16.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
                }
                Spotlight.NONE -> {}
            }
        }

        // Preview notes layered inside spotlight when NOTES step — real swipe, no DB
        if (step.id == TutorialStepId.NOTES && notesRect != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = with(density) { notesRect.left.toDp() } + 0.dp,
                        top = with(density) { notesRect.top.toDp() } + 0.dp,
                        end = with(density) { (maxW - notesRect.right).toDp() } + 0.dp
                    )
                    .padding(horizontal = 0.dp)
            ) {
                // Center preview vertically inside notes rect
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val previewScope = rememberCoroutineScope()
                    // Single interactive preview card — swipe advances
                    TutorialPreviewCard(
                        note = tutorialPreviewNotes.first(),
                        isAnimationsEnabled = isAnimationsEnabled,
                        onSwiped = {
                            if (step.requiresAction == RequiredAction.SWIPE_NOTE) {
                                onStepSwiped?.invoke()
                                previewScope.launch {
                                    delay(200)
                                    if (idx == 2) idx++
                                }
                            }
                        }
                    )
                    if (isAnimationsEnabled) {
                        Text(
                            "Swipe left or right on the card",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.align(Alignment.CenterHorizontally).background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Bubble
        val bubbleAlignment = when (step.spotlight) {
            Spotlight.PILL -> Alignment.BottomCenter
            Spotlight.NOTES -> Alignment.BottomCenter
            Spotlight.NONE -> Alignment.Center
        }
        // For NOTES, bubble stays above pill but below notes spotlight; for PILL, above pill
        val bubblePadding = when (step.spotlight) {
            Spotlight.PILL -> PaddingValues(bottom = 96.dp, start = 16.dp, end = 16.dp)
            Spotlight.NOTES -> PaddingValues(bottom = 96.dp, start = 16.dp, end = 16.dp)
            Spotlight.NONE -> PaddingValues(horizontal = 16.dp, vertical = 16.dp)
        }

        Box(modifier = Modifier.fillMaxSize().padding(bubblePadding), contentAlignment = bubbleAlignment) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Fixed 92dp text slot
                Box(modifier = Modifier.fillMaxWidth().height(92.dp), contentAlignment = Alignment.Center) {
                    AnimatedContent(targetState = step, label = "tutorialStep") { s ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Icon(s.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.height(6.dp))
                            Text(s.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center, maxLines = 1)
                            Spacer(Modifier.height(6.dp))
                            Text(s.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, lineHeight = 16.sp, maxLines = 3)
                        }
                    }
                }
                // Visual slot 72dp — contextual to step
                Box(
                    modifier = Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (step.id) {
                        TutorialStepId.WELCOME -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.StickyNote2, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Icon(Icons.Default.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Box(Modifier.width(96.dp).height(28.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
                                Text("Pill", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        TutorialStepId.PILL -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TouchApp, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("Tap the Pill below", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                            Icon(Icons.Default.Swipe, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TutorialStepId.NOTES -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.errorContainer).padding(6.dp), contentAlignment = Alignment.Center) { Icon(Icons.Default.Delete, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onErrorContainer) }
                            Text("← swipe →", style = MaterialTheme.typography.labelSmall)
                            Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.tertiaryContainer).padding(6.dp), contentAlignment = Alignment.Center) { Icon(Icons.Default.Lock, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer) }
                        }
                        TutorialStepId.MAPS_SEARCH -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Map, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("Maps", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.Search, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Search • tags", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    tutorialSteps.forEachIndexed { i, _ ->
                        val targetW = if (i == idx) 18.dp else 6.dp
                        val w by animateDpAsState(targetValue = targetW, animationSpec = if (isAnimationsEnabled) spring(0.8f, 300f) else tween(0), label = "dotW")
                        Box(Modifier.size(w, 6.dp).clip(CircleShape).background(if (i == idx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Skip") }
                    if (!isFirst) {
                        OutlinedButton(onClick = { idx-- }) { Text("Back") }
                    }
                    when (step.requiresAction) {
                        RequiredAction.TAP_PILL -> Text("Do it on the Pill →", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        RequiredAction.SWIPE_NOTE -> Text("Swipe the preview →", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        RequiredAction.NONE -> Button(onClick = { if (isLast) onDismiss() else idx++ }) { Text(if (isLast) "Done" else "Next") }
                    }
                }
            }
        }
        if (step.spotlight == Spotlight.PILL) {
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 76.dp)) { Icon(Icons.Default.ArrowDropDown, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
        }
    }
}

@Composable
private fun TutorialPreviewCard(
    note: Note,
    isAnimationsEnabled: Boolean,
    onSwiped: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val dismissThreshold = 90f
    val bgColor = when {
        offsetX > dismissThreshold / 2 -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
        offsetX < -dismissThreshold / 2 -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(18.dp))
            Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(18.dp))
        }
        androidx.compose.material3.Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(if (offsetX != 0f) 8.dp else 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = with(LocalDensity.current) { offsetX.toDp() })
                .clip(RoundedCornerShape(16.dp))
                .pointerInput(note.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (kotlin.math.abs(offsetX) > dismissThreshold) {
                                onSwiped()
                            }
                            offsetX = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount).coerceIn(-220f, 220f)
                        }
                    )
                }
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(note.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f), maxLines = 1)
                    Icon(Icons.Default.PushPin, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(4.dp))
                Text(note.content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                Spacer(Modifier.height(6.dp))
                Text("#${note.tags.first()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
