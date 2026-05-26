package com.example.pup_lagoon_app.ui.components

import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun ZoomableBox(
    modifier: Modifier = Modifier,
    contentAspectRatio: Float,
    minScale: Float = 1.5f,
    maxScale: Float = 4f,
    initialScale: Float = 2f,
    initialCenterPixel: Offset? = null,
    targetCenterPixel: Offset? = null,
    contentFullSize: IntSize? = null,
    content: @Composable () -> Unit
) {
    var scale by remember { mutableFloatStateOf(initialScale) }
    var rawOffset by remember { mutableStateOf(Offset.Zero) }
    var initialized by remember { mutableStateOf(false) }
    var isInteracting by remember { mutableStateOf(false) }

    // Animate the offset for smooth panning when a stall is selected
    val animatedOffset by animateOffsetAsState(
        targetValue = rawOffset,
        animationSpec = tween(durationMillis = 800),
        label = "map_offset"
    )

    // Use raw offset while interacting for zero lag, animated offset otherwise
    val currentOffset = if (isInteracting) rawOffset else animatedOffset

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val containerWidth = maxWidth.value
        val containerHeight = maxHeight.value
        val density = LocalDensity.current.density

        if (containerWidth > 0 && containerHeight > 0) {
            val containerAspectRatio = containerWidth / containerHeight
            val safeContentAspectRatio = if (contentAspectRatio > 0) contentAspectRatio else 1f

            val (baseWidth, baseHeight) = if (safeContentAspectRatio > containerAspectRatio) {
                (containerHeight * safeContentAspectRatio) to containerHeight
            } else {
                containerWidth to (containerWidth / safeContentAspectRatio)
            }

            // Calculation helper
            fun calculateBoundOffset(targetPixel: Offset, fullSize: IntSize): Offset {
                val fullWidth = fullSize.width.toFloat()
                val fullHeight = fullSize.height.toFloat()

                val normalizedX = targetPixel.x / fullWidth
                val normalizedY = targetPixel.y / fullHeight

                val targetBaseX = (normalizedX - 0.5f) * baseWidth * density
                val targetBaseY = (normalizedY - 0.5f) * baseHeight * density

                val baseWidthPx = baseWidth * density
                val baseHeightPx = baseHeight * density
                val containerWidthPx = containerWidth * density
                val containerHeightPx = containerHeight * density

                val maxX = (baseWidthPx * scale - containerWidthPx).coerceAtLeast(0f) / (2f * scale)
                val maxY = (baseHeightPx * scale - containerHeightPx).coerceAtLeast(0f) / (2f * scale)

                return Offset(
                    targetBaseX.coerceIn(-maxX, maxX),
                    targetBaseY.coerceIn(-maxY, maxY)
                )
            }

            // Initial positioning
            LaunchedEffect(containerWidth, containerHeight, contentFullSize, initialCenterPixel) {
                if (!initialized && contentFullSize != null && initialCenterPixel != null) {
                    rawOffset = calculateBoundOffset(initialCenterPixel, contentFullSize)
                    initialized = true
                }
            }

            // Handle target jumps (clicks)
            LaunchedEffect(targetCenterPixel, contentFullSize, scale) {
                if (targetCenterPixel != null && contentFullSize != null) {
                    rawOffset = calculateBoundOffset(targetCenterPixel, contentFullSize)
                }
            }

            Box(
                modifier = Modifier
                    .requiredSize(width = baseWidth.dp, height = baseHeight.dp)
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            isInteracting = true
                            val oldScale = scale
                            val newScale = (scale * zoom).coerceIn(minScale, maxScale)
                            scale = newScale

                            val newOffset = (rawOffset + centroid / oldScale) - (centroid / newScale + pan / oldScale)

                            val baseWidthPx = baseWidth * density
                            val baseHeightPx = baseHeight * density
                            val containerWidthPx = containerWidth * density
                            val containerHeightPx = containerHeight * density

                            val maxX = (baseWidthPx * scale - containerWidthPx).coerceAtLeast(0f) / (2f * scale)
                            val maxY = (baseHeightPx * scale - containerHeightPx).coerceAtLeast(0f) / (2f * scale)

                            rawOffset = Offset(
                                newOffset.x.coerceIn(-maxX, maxX),
                                newOffset.y.coerceIn(-maxY, maxY)
                            )
                        }
                        // Reset interaction state after gestures end
                        isInteracting = false
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = -currentOffset.x * scale
                        translationY = -currentOffset.y * scale
                    }
            ) {
                content()

                // Pin Overlay
                if (targetCenterPixel != null && contentFullSize != null) {
                    val fullWidth = contentFullSize.width.toFloat()
                    val fullHeight = contentFullSize.height.toFloat()

                    val pinX = (targetCenterPixel.x / fullWidth) * baseWidth * density
                    val pinY = (targetCenterPixel.y / fullHeight) * baseHeight * density

                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Stall Pin",
                        tint = Color.Red,
                        modifier = Modifier
                            .size(32.dp)
                            .graphicsLayer {
                                translationX = pinX - 16.dp.toPx()
                                translationY = pinY - 32.dp.toPx()
                            }
                    )
                }
            }
        }
    }
}
