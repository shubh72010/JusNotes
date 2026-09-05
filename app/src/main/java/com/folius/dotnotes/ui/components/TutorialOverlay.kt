package com.folius.dotnotes.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class TutorialStep(
    val title: String,
    val desc: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val spotlight: Spotlight?,
    val interactive: Boolean = false
)

enum class Spotlight { PILL, NOTES }

private val steps = listOf(
    TutorialStep("Welcome to DotNotes", "Pill-centric notes — tap to start, then use the real controls.", Icons.Default.Hub, null, false),
    TutorialStep("Pinned on top", "Pinned notes stay on top. Tap to open, long-press for more.", Icons.Default.PushPin, Spotlight.NOTES, false),
    TutorialStep("Swipe a note", "Swipe right → trash, left → lock. Try it on the card above.", Icons.Default.Swipe, Spotlight.NOTES, false),
    TutorialStep("Now the pill — TAP IT", "Tap the pill below. You must expand it to continue.", Icons.Default.TouchApp, Spotlight.PILL, true),
    TutorialStep("Close it", "Nice. Drag it down or tap outside to close and finish.", Icons.Default.PanTool, Spotlight.PILL, true),
)

@Composable
fun VisualTutorialOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isPillExpanded: Boolean = false,
    onStepAdvance: (() -> Unit)? = null
) {
    var idx by remember { mutableIntStateOf(0) }
    val step = steps[idx]
    val isLast = idx == steps.lastIndex
    val density = LocalDensity.current

    // Auto-advance when user actually uses the pill — MAKE THEM USE IT
    LaunchedEffect(idx, isPillExpanded) {
        if (step.interactive && step.spotlight == Spotlight.PILL) {
            // step 3: needs expand, step 4: needs collapse/expand toggle
            if (idx == 3 && isPillExpanded) { kotlinx.coroutines.delay(300); idx++ }
            if (idx == 4 && !isPillExpanded) { kotlinx.coroutines.delay(300); idx++ } // closed again after drag
        }
    }

    // For interactive pill steps, don't consume clicks — let the real pill underneath receive them
    val tappable = !step.interactive
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .then(if (tappable) Modifier.clickable { if (isLast) onDismiss() else idx++ } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        val maxW = constraints.maxWidth.toFloat()
        val maxH = constraints.maxHeight.toFloat()
        val pillW = maxW - with(density) { 32.dp.toPx() }
        val pillH = with(density) { 56.dp.toPx() }
        val pillX = (maxW - pillW) / 2f
        val pillY = maxH - with(density) { 12.dp.toPx() } - pillH - with(density) { 8.dp.toPx() }
        val notesTop = with(density) { 72.dp.toPx() }
        val notesH = with(density) { 160.dp.toPx() }
        val notesY = notesTop

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(Color.Black.copy(alpha = 0.64f))
            when (step.spotlight) {
                Spotlight.PILL -> drawRoundRect(Color.Transparent, Offset(pillX, pillY), Size(pillW, pillH), CornerRadius(28.dp.toPx(), 28.dp.toPx()), blendMode = BlendMode.Clear)
                Spotlight.NOTES -> drawRoundRect(Color.Transparent, Offset(with(density){12.dp.toPx()}, notesY), Size(maxW - with(density){24.dp.toPx()}, notesH), CornerRadius(16.dp.toPx(), 16.dp.toPx()), blendMode = BlendMode.Clear)
                null -> {}
            }
            when (step.spotlight) {
                Spotlight.PILL -> drawRoundRect(Color.White.copy(alpha = 0.35f), Offset(pillX, pillY), Size(pillW, pillH), CornerRadius(28.dp.toPx(), 28.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                Spotlight.NOTES -> drawRoundRect(Color.White.copy(alpha = 0.25f), Offset(with(density){12.dp.toPx()}, notesY), Size(maxW - with(density){24.dp.toPx()}, notesH), CornerRadius(16.dp.toPx(), 16.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
                null -> {}
            }
        }

        // Fixed-height bubble so steps don't jump — 188dp content area
        val bubbleAlignment = when (step.spotlight) {
            Spotlight.PILL -> Alignment.BottomCenter
            Spotlight.NOTES -> Alignment.TopCenter
            null -> Alignment.Center
        }
        val bubblePadding = when (step.spotlight) {
            Spotlight.PILL -> PaddingValues(bottom = 96.dp, start = 16.dp, end = 16.dp)
            Spotlight.NOTES -> PaddingValues(top = 260.dp, start = 16.dp, end = 16.dp)
            null -> PaddingValues(horizontal = 16.dp)
        }

        Box(modifier = Modifier.fillMaxSize().padding(bubblePadding), contentAlignment = bubbleAlignment) {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Fixed 92dp text slot so height never shifts
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
                Box(
                    modifier = Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (idx) {
                        0 -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) { Text("DN", color = Color.White, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold)) }
                            Box(Modifier.width(96.dp).height(28.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) { Text("pill", style = MaterialTheme.typography.labelSmall) }
                        }
                        1 -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.PushPin, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("pinned → top", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.Swipe, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("swipe the card", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        2 -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.errorContainer).padding(6.dp), contentAlignment = Alignment.Center) { Icon(Icons.Default.Delete, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onErrorContainer) }
                            Text("← swipe →", style = MaterialTheme.typography.labelSmall)
                            Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.tertiaryContainer).padding(6.dp), contentAlignment = Alignment.Center) { Icon(Icons.Default.Lock, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer) }
                        }
                        else -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Icon(if (idx == 3) Icons.Default.TouchApp else Icons.Default.PanTool, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(if (idx == 3) "tap the pill below" else "drag down to close", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    steps.forEachIndexed { i, _ ->
                        val w by animateDpAsState(if (i == idx) 18.dp else 6.dp, spring(0.8f, 300f), label = "dotW")
                        Box(Modifier.size(w, 6.dp).clip(CircleShape).background(if (i == idx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = onDismiss) { Text("Skip") }
                    if (step.interactive) {
                        Text("Do it on the pill →", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Button(onClick = { if (isLast) onDismiss() else idx++ }) { Text(if (isLast) "Done" else "Next") }
                    }
                }
            }
        }
        if (step.spotlight == Spotlight.PILL) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 76.dp)) { Icon(Icons.Default.ArrowDropDown, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
    }
}
