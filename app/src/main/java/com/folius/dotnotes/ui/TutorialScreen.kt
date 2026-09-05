package com.folius.dotnotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.folius.dotnotes.ui.components.VisualTutorialOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(onBack: () -> Unit) {
    val outerBg = MaterialTheme.colorScheme.background
    Scaffold(containerColor = outerBg) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(outerBg).padding(16.dp)) {
            Surface(shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxSize()) {
                VisualTutorialOverlay(onDismiss = onBack, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
