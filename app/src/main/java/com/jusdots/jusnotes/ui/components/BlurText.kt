package com.jusdots.jusnotes.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BlurText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    delay: Long = 200,
    animateByWords: Boolean = true
) {
    // Bypass for long text to prevent performance issues (ANRs)
    if (text.length > 1000) {
        Text(
            text = text,
            style = style,
            modifier = modifier
        )
        return
    }

    // Split into words but preserve whitespace/newlines
    // This regex splits by spaces but keeps the delimiter (the space)
    val elements = if (animateByWords) {
        text.split(Regex("(?<=\\s)|(?=\\s)")).filter { it.isNotEmpty() }
    } else {
        text.map { it.toString() }
    }
    
    FlowRow(modifier = modifier) {
        elements.forEachIndexed { index, element ->
            if (element == "\n") {
                // Force a newline in FlowRow by taking up the full width
                Spacer(modifier = Modifier.fillMaxWidth().height(0.dp))
            } else if (element.isBlank()) {
                // Render space with same style to preserve width
                Text(
                    text = element,
                    style = style,
                    modifier = Modifier.alpha(0f) // Invisible space
                )
            } else {
                AnimatedWord(
                    text = element,
                    style = style,
                    delay = index * delay.toInt()
                )
            }
        }
    }
}

@Composable
private fun AnimatedWord(
    text: String,
    style: TextStyle,
    delay: Int
) {
    val alpha = remember { Animatable(0f) }
    val translationY = remember { Animatable(12f) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        launch {
            alpha.animateTo(1f, animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing))
        }
        launch {
            translationY.animateTo(0f, animationSpec = tween(durationMillis = 400, easing = BackOutEasing))
        }
    }

    Text(
        text = text,
        style = style,
        modifier = Modifier
            .graphicsLayer { this.translationY = translationY.value }
            .alpha(alpha.value)
    )
}

// Easing for "pop"
val BackOutEasing = Easing { x -> 
    val c1 = 1.70158f;
    val c3 = c1 + 1f;
    1f + c3 * (x - 1f).let { it * it * it } + c1 * (x - 1f).let { it * it }
}
