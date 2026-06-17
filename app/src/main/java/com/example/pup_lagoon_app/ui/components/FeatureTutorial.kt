package com.example.pup_lagoon_app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.example.pup_lagoon_app.ui.theme.PuplagoonappTheme
import com.example.pup_lagoon_app.ui.theme.Maroon

@Composable
fun FeatureTutorial(
    step: Int,
    targetBounds: Rect?,
    onSkip: () -> Unit,
    isVisible: Boolean,
    content: @Composable () -> Unit
) {
    val tutorialData = listOf(
        TutorialStep(
            title = "Explore the Lagoon",
            description = "Start by panning the map to see different areas of the PUP Lagoon.",
            alignment = Alignment.TopCenter,
            offsetY = 10.dp
        ),
        TutorialStep(
            title = "Zoom for Details",
            description = "Pinch to zoom in and see individual stalls and landmarks more clearly.",
            alignment = Alignment.TopCenter,
            offsetY = 10.dp
        ),
        TutorialStep(
            title = "Quick Search",
            description = "Tap the search bar to find specific food items.",
            alignment = Alignment.Center,
            offsetY = 0.dp
        ),
        TutorialStep(
            title = "Find Your Craving",
            description = "Try typing 'burger' to see what's cooking in the lagoon.",
            alignment = Alignment.Center,
            offsetY = 0.dp
        ),
        TutorialStep(
            title = "Select a Stall",
            description = "Tap on the first result to see more details about the stall.",
            alignment = Alignment.Center,
            offsetY = 0.dp
        ),
        TutorialStep(
            title = "Explore More",
            description = "Swipe up on the stall card to reveal the full menu and photos.",
            alignment = Alignment.TopCenter,
            offsetY = 10.dp
        ),
        TutorialStep(
            title = "Keep it Clean",
            description = "Swipe down or tap the map to minimize the stall details and get back to exploring.",
            alignment = Alignment.BottomCenter,
            offsetY = (-10).dp
        ),
        TutorialStep(
            title = "Find Your Way",
            description = "Tap on 'Gate 1' to get instant directions from the entrance to this stall.",
            alignment = Alignment.Center,
            offsetY = 0.dp
        ),
        TutorialStep(
            title = "Focus on Map",
            description = "Minimize the directions guidance to have a better view of the path.",
            alignment = Alignment.Center,
            offsetY = 0.dp
        ),
        TutorialStep(
            title = "Keep for Later",
            description = "Tap the pin icon to 'keep' this stall marked on your map for future visits.",
            alignment = Alignment.Center,
            offsetY = 0.dp
        ),
        TutorialStep(
            title = "You're All Set!",
            description = "You've mastered the lagoon map. Happy food hunting!",
            alignment = Alignment.Center,
            offsetY = 0.dp
        )
    )

    val currentStep = tutorialData.getOrElse(step) { tutorialData.last() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 1: App Content with Blocker
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isVisible) {
                        Modifier.pointerInput(targetBounds, step) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val position = event.changes.first().position
                                    
                                    if (targetBounds != null) {
                                        val center = targetBounds.center
                                        val radius = (kotlin.math.max(targetBounds.width, targetBounds.height) / 2f) + 30.dp.toPx()
                                        val distance = (position - center).getDistance()
                                        
                                        // If click is OUTSIDE the spotlight, consume it to block interaction
                                        if (distance > radius) {
                                            event.changes.forEach { it.consume() }
                                        }
                                        // If INSIDE, do nothing, allowing the event to reach the app below
                                    } else if (step > 1 && step < 10 && step != 6) {
                                        // Block everything if a target is expected but not yet found
                                        // EXCEPT for step 6 (Step 7: Keep it Clean) which allows map/sheet interaction
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                        }
                    } else Modifier
                )
        ) {
            content()
        }

        // Layer 2: Tutorial Overlay
        if (isVisible) {
            // Spotlight Overlay Visuals
            val spotlightProgress by animateFloatAsState(
                targetValue = if (targetBounds != null) 1f else 0f,
                animationSpec = tween(500),
                label = "spotlight_alpha"
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            ) {
                // Dimming overlay
                drawRect(color = Color.Black.copy(alpha = 0.5f * spotlightProgress))
                
                if (targetBounds != null) {
                    val center = targetBounds.center
                    val radius = (kotlin.math.max(targetBounds.width, targetBounds.height) / 2f) + 30.dp.toPx()
                    
                    // Clear the spotlight area
                    drawCircle(
                        color = Color.Transparent,
                        radius = radius,
                        center = center,
                        blendMode = BlendMode.Clear
                    )

                    // High-contrast double border
                    drawCircle(
                        color = Color.White,
                        radius = radius,
                        center = center,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    
                    drawCircle(
                        color = Maroon,
                        radius = radius + 2.dp.toPx(),
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            // Tutorial Instruction Card
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(300, delayMillis = 150)) + 
                     slideInVertically(initialOffsetY = { if (it > 0) it / 2 else -it / 2 }))
                        .togetherWith(fadeOut(animationSpec = tween(150)))
                },
                modifier = Modifier
                    .align(currentStep.alignment)
                    .offset(y = currentStep.offsetY)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                label = "tutorial_card"
            ) { stepIndex ->
                val stepData = tutorialData.getOrElse(stepIndex) { tutorialData.last() }
                
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp)
                        .shadow(16.dp, RoundedCornerShape(20.dp)),
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Maroon.copy(alpha = 0.8f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Step Number Badge
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Maroon, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${stepIndex + 1}",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stepData.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Maroon
                            )
                            Text(
                                text = stepData.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.DarkGray,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        // Close Button
                        IconButton(
                            onClick = onSkip,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Skip Tutorial",
                                tint = Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Pulse animation for visual cue when no target is specified
            if (targetBounds == null && (step == 0 || step == 1)) {
                 PulseIndicator(
                     Modifier
                         .align(Alignment.Center)
                         .size(160.dp)
                 )
            }
        }
    }
}

@Composable
fun PulseIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .background(Maroon.copy(alpha = 0.4f), CircleShape)
    )
}

@Preview(showBackground = true)
@Composable
fun FeatureTutorialPreview() {
    PuplagoonappTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            FeatureTutorial(
                step = 0,
                targetBounds = Rect(offset = Offset(400f, 200f), size = Size(200f, 200f)),
                onSkip = {},
                isVisible = true
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.White))
            }
        }
    }
}

data class TutorialStep(
    val title: String,
    val description: String,
    val alignment: Alignment,
    val offsetY: androidx.compose.ui.unit.Dp
)
