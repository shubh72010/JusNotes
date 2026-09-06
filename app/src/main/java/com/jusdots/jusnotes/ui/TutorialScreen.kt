package com.jusdots.jusnotes.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jusdots.jusnotes.ui.components.tutorialSteps
import com.jusdots.jusnotes.ui.components.TutorialStepId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(onBack: () -> Unit) {
    val outerBg = MaterialTheme.colorScheme.background
    var idx by remember { mutableIntStateOf(0) }
    val step = tutorialSteps[idx]
    val isLast = idx == tutorialSteps.lastIndex
    val isFirst = idx == 0

    Scaffold(containerColor = outerBg) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(outerBg).padding(16.dp)) {
            Surface(shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Text(
                        "How JusNotes works",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.6.sp, fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(16.dp))

                    // Pager content — AnimatedContent respects reduced motion by using tween 0 if needed
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        AnimatedContent(targetState = step, label = "tutorialPager") { s ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(s.icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(32.dp))
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(s.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                                Spacer(Modifier.height(8.dp))
                                Text(s.desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, lineHeight = 20.sp, modifier = Modifier.padding(horizontal = 8.dp))
                                Spacer(Modifier.height(20.dp))
                                // Visual slot — contextual illustration (no DB)
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(88.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (s.id) {
                                        TutorialStepId.WELCOME -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.StickyNote2, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(20.dp))
                                            }
                                            Icon(Icons.Default.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Box(Modifier.width(96.dp).height(28.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
                                                Text("Pill", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                        TutorialStepId.PILL -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.TouchApp, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                            Text("Tap • Swipe • Drag", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                                        }
                                        TutorialStepId.NOTES -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.errorContainer).padding(8.dp), contentAlignment = Alignment.Center) { Icon(Icons.Default.Delete, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onErrorContainer) }
                                            Text("swipe", style = MaterialTheme.typography.labelSmall)
                                            Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.tertiaryContainer).padding(8.dp), contentAlignment = Alignment.Center) { Icon(Icons.Default.Lock, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer) }
                                        }
                                        TutorialStepId.MAPS_SEARCH -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Map, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                            Text("Maps", style = MaterialTheme.typography.labelSmall)
                                            Spacer(Modifier.width(8.dp))
                                            Icon(Icons.Default.Search, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("Search & tags", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        tutorialSteps.forEachIndexed { i, _ ->
                            val w by animateDpAsState(if (i == idx) 18.dp else 6.dp, spring(0.8f, 300f), label = "dotW")
                            Box(Modifier.size(w, 6.dp).clip(CircleShape).background(if (i == idx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Close") }
                        if (!isFirst) {
                            OutlinedButton(onClick = { idx-- }, modifier = Modifier.weight(1f)) { Text("Back") }
                        }
                        Button(onClick = { if (isLast) onBack() else idx++ }, modifier = Modifier.weight(1f)) { Text(if (isLast) "Done" else "Next") }
                    }
                }
            }
        }
    }
}
