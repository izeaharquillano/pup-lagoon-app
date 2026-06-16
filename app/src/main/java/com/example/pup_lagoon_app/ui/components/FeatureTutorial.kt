package com.example.pup_lagoon_app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pup_lagoon_app.ui.theme.Maroon

@Composable
fun FeatureTutorial(
    step: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val tutorialData = listOf(
        TutorialStep(
            title = "Search & Filter",
            description = "Find your favorite food or stalls using the search bar. Use filters to narrow down by category or price.",
            alignment = Alignment.TopCenter,
            offsetY = 120.dp
        ),
        TutorialStep(
            title = "Interactive Map",
            description = "Pinch to zoom and drag to explore. Tap on any red pin to see stall details and menus.",
            alignment = Alignment.Center,
            offsetY = 0.dp
        ),
        TutorialStep(
            title = "Smart Directions",
            description = "Need help finding a stall? Tap on a Gate icon (Gate 1, 2, or 3) to see the fastest route.",
            alignment = Alignment.Center,
            offsetY = 100.dp
        ),
        TutorialStep(
            title = "Stall Details",
            description = "The bottom sheet reveals everything you need: photos, menus, and the option to save your favorites.",
            alignment = Alignment.BottomCenter,
            offsetY = (-120).dp
        )
    )

    val currentStep = tutorialData[step]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(enabled = false) {} // Prevent clicking elements behind
    ) {
        // Skip Button
        IconButton(
            onClick = onSkip,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 16.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Skip Tutorial", tint = Color.White)
        }

        // Highlight Effect (Abstracted for now, just using a centered card)
        Column(
            modifier = Modifier
                .align(currentStep.alignment)
                .offset(y = currentStep.offsetY)
                .padding(32.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Step ${step + 1} of 4",
                style = MaterialTheme.typography.labelMedium,
                color = Maroon,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = currentStep.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = currentStep.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = Color.DarkGray,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = Maroon),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    text = if (step < 3) "Next" else "Finish",
                    fontWeight = FontWeight.Bold
                )
                if (step < 3) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Pulse animation for visual cue (optional improvement)
        if (step == 1) { // Map step
             PulseIndicator(Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun PulseIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .size(80.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .background(Maroon, CircleShape)
    )
}

data class TutorialStep(
    val title: String,
    val description: String,
    val alignment: Alignment,
    val offsetY: androidx.compose.ui.unit.Dp
)
