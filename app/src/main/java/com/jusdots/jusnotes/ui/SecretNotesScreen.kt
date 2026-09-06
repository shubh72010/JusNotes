package com.jusdots.jusnotes.ui

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.jusdots.jusnotes.ui.components.DecryptedText
import android.content.Context
import android.content.ContextWrapper
import com.jusdots.jusnotes.data.Note

private fun Context.findActivity(): FragmentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is FragmentActivity) return context
        context = context.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretNotesScreen(
    viewModel: NoteViewModel,
    onBack: () -> Unit,
    onNoteClick: (Int) -> Unit
) {
    val isAuthenticated by viewModel.isSecretAuthenticated.collectAsState()
    var authTrigger by remember { mutableIntStateOf(0) }
    var showBiometricUnavailableDialog by remember { mutableStateOf(false) }
    var isPillMenuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val secretNotes by viewModel.secretNotes.collectAsState()
    val animationsEnabled by viewModel.isAnimationsEnabled.collectAsState()
    
    // Check if biometric is available
    val biometricManager = remember(context) { BiometricManager.from(context) }
    val canAuthenticate = remember(biometricManager) {
        biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
    }
    
    val currentContext by rememberUpdatedState(context)
    val currentViewModel by rememberUpdatedState(viewModel)
    
    val triggerBiometricAuth = remember(canAuthenticate) {
        {
            if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
                val activity = currentContext.findActivity()
                if (activity != null) {
                    val executor = ContextCompat.getMainExecutor(currentContext)
                    val callback = object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            currentViewModel.setSecretAuthenticated(true)
                        }
                        
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            Toast.makeText(currentContext, errString, Toast.LENGTH_SHORT).show()
                        }
                        
                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            // System handles retries
                        }
                    }
                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Unlock Secret Notes")
                        .setSubtitle("Authenticate to access your hidden notes")
                        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                        .build()
                        
                    BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
                }
            } else {
                 showBiometricUnavailableDialog = true
            }
        }
    }

    // Manual trigger via button
    LaunchedEffect(authTrigger) {
        if (authTrigger > 0 && !isAuthenticated) {
            triggerBiometricAuth()
        }
    }

    // Initial trigger on entry
    LaunchedEffect(Unit) {
        if (!isAuthenticated) {
            triggerBiometricAuth()
        }
    }

    if (showBiometricUnavailableDialog) {
        AlertDialog(
            onDismissRequest = { showBiometricUnavailableDialog = false },
            title = { Text("Biometrics Unavailable") },
            text = { Text("Biometric authentication (or device credential) is not set up or available on this device. You won't be able to access secret notes.") },
            confirmButton = {
                TextButton(onClick = { 
                    showBiometricUnavailableDialog = false
                    onBack()
                }) {
                    Text("Go Back")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBiometricUnavailableDialog = false }) {
                    Text("Stay Here")
                }
            }
        )
    }

    val outerBg = MaterialTheme.colorScheme.background
    val cardBg = MaterialTheme.colorScheme.surfaceContainer
    val pillBg = MaterialTheme.colorScheme.surfaceContainerHighest
    Scaffold(containerColor = outerBg) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(outerBg).padding(horizontal = 16.dp, vertical = 12.dp)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(shape = RoundedCornerShape(32.dp), color = cardBg, modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (!isAuthenticated) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Lock, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Authentication Required", style = MaterialTheme.typography.titleMedium.copy(color = Color.White))
                                    Button(onClick = { authTrigger++ }, modifier = Modifier.padding(top = 16.dp)) { Text("Unlock") }
                                }
                            }
                        } else {
                            if (secretNotes.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No secret notes found.", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) }
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize().padding(20.dp), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(secretNotes, key = { it.id }) { note ->
                                        SecretNoteItem(note = note, animationsEnabled = animationsEnabled, onClick = { onNoteClick(note.id) })
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))

                val secH by androidx.compose.animation.core.animateDpAsState(targetValue = if (isPillMenuExpanded) 200.dp else 56.dp, animationSpec = androidx.compose.animation.core.spring(dampingRatio=0.85f, stiffness=300f), label="secH")
                val secCorner by androidx.compose.animation.core.animateDpAsState(targetValue = if (isPillMenuExpanded) 32.dp else 28.dp, animationSpec = androidx.compose.animation.core.spring(dampingRatio=0.85f, stiffness=300f), label="secCorner")
                val secS = RoundedCornerShape(secCorner)
                Surface(shape = secS, color = pillBg, modifier = Modifier.fillMaxWidth().height(secH)
                    .pointerInput(Unit) {
                        var dX = 0f
                        var dY = 0f
                        detectDragGestures(
                            onDragEnd = {
                                if (isPillMenuExpanded && dY > 80) isPillMenuExpanded = false
                                else if (dY < -80) isPillMenuExpanded = true
                                else if (kotlin.math.abs(dX)>80) onBack()
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
                    androidx.compose.animation.AnimatedContent(targetState = isPillMenuExpanded, label="secMenu") { exp ->
                        if (exp) {
                            Column(modifier=Modifier.fillMaxSize().padding(16.dp), horizontalAlignment=Alignment.CenterHorizontally) {
                                Box(modifier=Modifier.width(32.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha=0.3f)))
                                Spacer(modifier=Modifier.height(12.dp))
                                if (!isAuthenticated) { Button(onClick={ isPillMenuExpanded=false; authTrigger++ }, modifier=Modifier.fillMaxWidth()) { Text("Unlock") } }
                                OutlinedButton(onClick={ isPillMenuExpanded=false; onBack() }, modifier=Modifier.fillMaxWidth()) { Text("Back", color=Color.White) }
                                TextButton(onClick={isPillMenuExpanded=false}) { Text("Close", color=Color.White) }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(if (isAuthenticated) "secret notes" else "locked", style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface, textAlign = androidx.compose.ui.text.style.TextAlign.Center))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SecretNoteItem(note: Note, animationsEnabled: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            DecryptedText(
                text = if (note.title.isBlank()) "Untitled Secret" else note.title,
                style = MaterialTheme.typography.titleLarge,
                speed = if (animationsEnabled) 40 else 0,
                maxIterations = if (animationsEnabled) 10 else 0,
                encryptedColor = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            val previewText = if (note.isChecklist) {
                note.checklist.joinToString(", ") { it.text }
            } else {
                note.content
            }
            
            if (previewText.isNotBlank()) {
                DecryptedText(
                    text = previewText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    speed = if (animationsEnabled) 20 else 0,
                    maxIterations = if (animationsEnabled) 10 else 0,
                    encryptedColor = MaterialTheme.colorScheme.secondary,
                    characters = "01",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
