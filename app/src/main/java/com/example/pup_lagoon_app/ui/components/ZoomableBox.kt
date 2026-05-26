package com.example.pup_lagoon_app.ui.components

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun ZoomableBox(
    modifier: Modifier = Modifier,
    contentAspectRatio: Float,
    minScale: Float = 1f,
    maxScale: Float = 4f,
    initialScale: Float = 2f,
    initialCenterPixel: Offset? = null,
    contentFullSize: IntSize? = null,
    content: @Composable () -> Unit
) {
    var scale by remember { mutableFloatStateOf(initialScale) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var initialized by remember { mutableStateOf(false) }

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

            // Initialization logic for custom centering
            LaunchedEffect(containerWidth, containerHeight, contentFullSize, initialCenterPixel) {
                if (!initialized && contentFullSize != null && initialCenterPixel != null) {
                    val fullWidth = contentFullSize.width.toFloat()
                    val fullHeight = contentFullSize.height.toFloat()

                    // Normalize target pixel to [0, 1] range
                    val normalizedX = initialCenterPixel.x / fullWidth
                    val normalizedY = initialCenterPixel.y / fullHeight

                    // Map normalized coordinates to base coordinates (relative to center)
                    // Offset(0,0) is center. normalized (0.5, 0.5) is center.
                    val targetBaseX = (normalizedX - 0.5f) * baseWidth * density
                    val targetBaseY = (normalizedY - 0.5f) * baseHeight * density

                    // Initial bounding
                    val baseWidthPx = baseWidth * density
                    val baseHeightPx = baseHeight * density
                    val containerWidthPx = containerWidth * density
                    val containerHeightPx = containerHeight * density

                    val maxX = (baseWidthPx * scale - containerWidthPx).coerceAtLeast(0f) / (2f * scale)
                    val maxY = (baseHeightPx * scale - containerHeightPx).coerceAtLeast(0f) / (2f * scale)

                    offset = Offset(
                        targetBaseX.coerceIn(-maxX, maxX),
                        targetBaseY.coerceIn(-maxY, maxY)
                    )
                    initialized = true
                }
            }

            Box(
                modifier = Modifier
                    .requiredSize(width = baseWidth.dp, height = baseHeight.dp)
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val oldScale = scale
                            val newScale = (scale * zoom).coerceIn(minScale, maxScale)
                            scale = newScale

                            val newOffset = (offset + centroid / oldScale) - (centroid / newScale + pan / oldScale)

                            val baseWidthPx = baseWidth * density
                            val baseHeightPx = baseHeight * density
                            val containerWidthPx = containerWidth * density
                            val containerHeightPx = containerHeight * density

                            val maxX = (baseWidthPx * scale - containerWidthPx).coerceAtLeast(0f) / (2f * scale)
                            val maxY = (baseHeightPx * scale - containerHeightPx).coerceAtLeast(0f) / (2f * scale)

                            offset = Offset(
                                newOffset.x.coerceIn(-maxX, maxX),
                                newOffset.y.coerceIn(-maxY, maxY)
                            )
                        }
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = -offset.x * scale
                        translationY = -offset.y * scale
                    }
            ) {
                content()
            }
        }
    }
}
