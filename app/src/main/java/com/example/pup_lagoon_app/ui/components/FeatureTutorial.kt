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
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import com.example.pup_lagoon_app.ui.theme.PuplagoonappTheme
import com.example.pup_lagoon_app.ui.theme.Maroon

@Composable
fun FeatureTutorial(
    step: Int,
    targetBounds: Rect?,
    searchBarBounds: Rect? = null,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val tutorialData = listOf(
        TutorialStep(
            title = "Search & Filter",
            description = "Find your favorite food or stalls using the search bar. Use filters to narrow down by category or price.",
            alignment = Alignment.TopCenter,
            offsetY = 160.dp
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
            alignment = Alignment.TopCenter,
            offsetY = 160.dp
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
            .clickable(enabled = false) {} // Prevent clicking elements behind
    ) {
        // Spotlight Overlay
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
            drawRect(color = Color.Black.copy(alpha = 0.7f * spotlightProgress))
            
            if (targetBounds != null) {
                val center = targetBounds.center
                val radius = (kotlin.math.max(targetBounds.width, targetBounds.height) / 2f) + 20.dp.toPx()
                
                drawCircle(
                    color = Color.Transparent,
                    radius = radius,
                    center = center,
                    blendMode = BlendMode.Clear
                )
            }
        }

        // Skip Button
        val density = LocalDensity.current
        val skipButtonModifier = if (searchBarBounds != null) {
            val topPadding = with(density) { searchBarBounds.bottom.toDp() + 16.dp }
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = topPadding, end = 16.dp)
        } else {
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 16.dp)
        }

        IconButton(
            onClick = onSkip,
            modifier = skipButtonModifier
        ) {
            Icon(Icons.Default.Close, contentDescription = "Skip Tutorial", tint = Color.White)
        }

        // Tutorial Card
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                (fadeIn(animationSpec = tween(300, delayMillis = 150)) + 
                 slideInVertically(initialOffsetY = { it / 2 }))
                    .togetherWith(fadeOut(animationSpec = tween(150)))
            },
            modifier = Modifier
                .align(currentStep.alignment)
                .offset(y = currentStep.offsetY)
                .padding(32.dp),
            label = "tutorial_card"
        ) { stepData ->
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .shadow(12.dp),
                color = Color.White,
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
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
                        text = stepData.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stepData.description,
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
            }
        }

        // Pulse animation for visual cue
        if (targetBounds != null) {
             PulseIndicator(
                 Modifier
                     .offset { targetBounds.center.round() }
                     .size(100.dp)
                     .offset(x = (-50).dp, y = (-50).dp)
             )
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

@Preview(showBackground = true)
@Composable
fun FeatureTutorialPreview() {
    PuplagoonappTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            // Mock content behind the tutorial
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = 60.dp)
                    .background(Color.Red)
            )
            
            FeatureTutorial(
                step = 0,
                targetBounds = Rect(offset = Offset(400f, 200f), size = Size(200f, 200f)),
                searchBarBounds = Rect(offset = Offset(0f, 100f), size = Size(1080f, 150f)),
                onNext = {},
                onSkip = {}
            )
        }
    }
}

data class TutorialStep(
    val title: String,
    val description: String,
    val alignment: Alignment,
    val offsetY: androidx.compose.ui.unit.Dp
)
