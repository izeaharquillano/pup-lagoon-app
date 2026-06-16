package com.example.pup_lagoon_app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pup_lagoon_app.data.LabelType
import com.example.pup_lagoon_app.data.MapLabel

@Composable
fun ZoomableBox(
    modifier: Modifier = Modifier,
    contentAspectRatio: Float,
    minScale: Float = 1.25f,
    maxScale: Float = 4f,
    initialScale: Float = 2.0f,
    initialCenterPixel: Offset? = null,
    targetCenterPixel: Offset? = null,
    selectedStallIds: Set<String> = emptySet(),
    selectedStallLocations: Map<String, Offset> = emptyMap(),
    contentFullSize: IntSize? = null,
    keptPins: Map<String, Offset> = emptyMap(),
    mapLabels: List<MapLabel> = emptyList(),
    navigationPath: List<Offset> = emptyList(),
    selectedGateId: String? = null,
    onPinClick: ((String) -> Unit)? = null,
    onLandmarkClick: ((String) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onInteraction: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(initialScale) }
    var rawOffset by remember { mutableStateOf(Offset.Zero) }
    var initialized by remember { mutableStateOf(false) }
    var isInteracting by remember { mutableStateOf(value = false) }

    // Animate the offset for smooth panning when a stall is selected
    val animatedOffset by animateOffsetAsState(
        targetValue = rawOffset,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "map_offset"
    )

    // Animate the scale for smooth zooming
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "map_scale"
    )

    // Use raw values while interacting for zero lag, animated values otherwise
    val currentOffset = if (isInteracting) rawOffset else animatedOffset
    val currentScale = if (isInteracting) scale else animatedScale

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val containerWidth = maxWidth.value
        val containerHeight = maxHeight.value
        val density = LocalDensity.current.density

        if ((containerWidth > 0) && (containerHeight > 0)) {
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

            LaunchedEffect(targetCenterPixel, contentFullSize) {
                if (targetCenterPixel != null && contentFullSize != null) {
                    // Force interaction to false so we use animated values for the jump
                    isInteracting = false
                    rawOffset = calculateBoundOffset(targetCenterPixel, contentFullSize)
                    // Reset zoom to initialScale when selecting a new stall
                    scale = initialScale
                }
            }

            Box(
                modifier = Modifier
                    .requiredSize(width = baseWidth.dp, height = baseHeight.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onClick?.invoke() }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            isInteracting = true
                            onInteraction?.invoke()

                            val oldScale = scale
                            val newScale = (scale * zoom).coerceIn(minScale, maxScale)
                            
                            val baseWidthPx = baseWidth * density
                            val baseHeightPx = baseHeight * density
                            
                            // Centroid relative to the center of the content box
                            val relativeCentroid = Offset(
                                centroid.x - baseWidthPx / 2f,
                                centroid.y - baseHeightPx / 2f
                            )

                            // Correctly calculate new offset to keep the point under the fingers
                            val newOffset = (rawOffset + relativeCentroid / oldScale) - 
                                           (relativeCentroid / newScale + pan / oldScale)

                            scale = newScale

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
                        scaleX = currentScale
                        scaleY = currentScale
                        translationX = -currentOffset.x * currentScale
                        translationY = -currentOffset.y * currentScale
                    }
            ) {
                content()

                // Render Route Guidance Path
                if (contentFullSize != null && navigationPath.isNotEmpty()) {
                    val fullWidth = contentFullSize.width.toFloat()
                    val fullHeight = contentFullSize.height.toFloat()

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val pathPoints = navigationPath.map { point ->
                            Offset(
                                (point.x / fullWidth) * baseWidth * density,
                                (point.y / fullHeight) * baseHeight * density
                            )
                        }

                        if (pathPoints.size >= 2) {
                            for (i in 0 until pathPoints.size - 1) {
                                val startPoint = pathPoints[i]
                                val endPoint = pathPoints[i + 1]
                                
                                // Calculate unit vector for offsets to prevent clipping into pins
                                val dx = endPoint.x - startPoint.x
                                val dy = endPoint.y - startPoint.y
                                val length = kotlin.math.sqrt(dx * dx + dy * dy)
                                
                                // Adjust offsets: Prevent bleeding below pin tip with a small start offset
                                val startOffset = (2.dp.toPx() / currentScale)
                                val endOffset = 0f
                                
                                if (length > (startOffset + endOffset)) {
                                    val ux = dx / length
                                    val uy = dy / length
                                    
                                    val adjustedStart = Offset(
                                        startPoint.x + ux * startOffset,
                                        startPoint.y + uy * startOffset
                                    )
                                    val adjustedEnd = Offset(
                                        endPoint.x - ux * endOffset,
                                        endPoint.y - uy * endOffset
                                    )

                                    drawLine(
                                        color = Color.Red,
                                        start = adjustedStart,
                                        end = adjustedEnd,
                                        strokeWidth = 3.dp.toPx() / currentScale,
                                        cap = StrokeCap.Round,
                                        pathEffect = PathEffect.dashPathEffect(
                                            floatArrayOf(10f / currentScale, 10f / currentScale),
                                            0f
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Render all kept pins
                if (contentFullSize != null) {
                    val fullWidth = contentFullSize.width.toFloat()
                    val fullHeight = contentFullSize.height.toFloat()

                    keptPins.forEach { (id, location) ->
                        // Skip if it's one of the currently selected stalls
                        if (id !in selectedStallIds) {
                            val pinX = (location.x / fullWidth) * baseWidth * density
                            val pinY = (location.y / fullHeight) * baseHeight * density

                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Kept Pin",
                                tint = Color(0xFFB71C1C),
                                modifier = Modifier
                                    .size(44.dp)
                                    .graphicsLayer {
                                        transformOrigin = TransformOrigin(0.5f, 1f)
                                        scaleX = 1f / currentScale
                                        scaleY = 1f / currentScale
                                        translationX = pinX - 22.dp.toPx()
                                        translationY = pinY - 44.dp.toPx()
                                    }
                                    .pointerInput(id) {
                                        detectTapGestures {
                                            onPinClick?.invoke(id)
                                        }
                                    }
                            )
                        }
                    }
                }

                // Selected Pins Overlay
                if (contentFullSize != null) {
                    val fullWidth = contentFullSize.width.toFloat()
                    val fullHeight = contentFullSize.height.toFloat()

                    selectedStallLocations.forEach { (id, location) ->
                        val pinX = (location.x / fullWidth) * baseWidth * density
                        val pinY = (location.y / fullHeight) * baseHeight * density

                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Selected Stall Pin",
                            tint = Color.Red,
                            modifier = Modifier
                                .background(Color.White, CircleShape)
                                .padding(2.dp)
                                .size(54.dp)
                                .graphicsLayer {
                                    // Set the origin of all transformations to the bottom-center tip
                                    transformOrigin = TransformOrigin(0.5f, 1f)

                                    // INVERSE SCALING:
                                    // This makes the pin stay the same visual size on the screen
                                    scaleX = 1f / currentScale
                                    scaleY = 1f / currentScale

                                    // Position the bottom-center tip at (pinX, pinY)
                                    // Since Box is TopStart, (0,0) is top-left.
                                    translationX = pinX - 27.dp.toPx()
                                    translationY = pinY - 54.dp.toPx()
                                }
                                .pointerInput(id) {
                                    detectTapGestures {
                                        onPinClick?.invoke(id)
                                    }
                                }
                        )
                    }

                    // Render Map Labels
                    mapLabels.forEach { label ->
                        val labelX = (label.pixelX / fullWidth) * baseWidth * density
                        val labelY = (label.pixelY / fullHeight) * baseHeight * density

                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                                    scaleX = 1f / currentScale
                                    scaleY = 1f / currentScale
                                    translationX = labelX - (100.dp.toPx() / 2f)
                                    translationY = labelY - (50.dp.toPx() / 2f)
                                }
                                .size(width = 100.dp, height = 50.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when (label.type) {
                                LabelType.BUILDING -> {
                                    Surface(
                                        color = Color.White.copy(alpha = 0.8f),
                                        shape = RoundedCornerShape(4.dp),
                                        shadowElevation = 2.dp
                                    ) {
                                        Text(
                                            text = label.text,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                            color = Color.Black
                                        )
                                    }
                                }
                                LabelType.STREET -> {
                                    Text(
                                        text = label.text,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontStyle = FontStyle.Italic,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = Color.DarkGray.copy(alpha = 0.7f),
                                        modifier = Modifier.rotate(label.rotation)
                                    )
                                }
                                LabelType.LANDMARK -> {
                                    val isSelected = label.id == selectedGateId
                                    val iconScale = if (isSelected) 1.5f else 1.0f
                                    
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .pointerInput(label.id) {
                                                detectTapGestures {
                                                    onLandmarkClick?.invoke(label.id)
                                                }
                                            }
                                    ) {
                                        // Text ABOVE icon with dynamic spacing
                                        Surface(
                                            color = Color.White.copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier
                                                .graphicsLayer {
                                                    // Move text above the center (coordinate)
                                                    // Base: -24dp (icon height) - 8dp (padding) = -32dp
                                                    // Selection: Shift up by 12dp to clear expanded icon (36dp + 8dp = 44dp)
                                                    translationY = if (isSelected) -44.dp.toPx() else -32.dp.toPx()
                                                }
                                        ) {
                                            Text(
                                                text = label.text,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = if (isSelected) 10.sp else 9.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                                ),
                                                color = if (isSelected) Color.Red else Color.Gray,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }

                                        Box(contentAlignment = Alignment.Center) {
                                            // White circle to fill the "hole" in the pin
                                            Box(
                                                modifier = Modifier
                                                    .size(9.dp)
                                                    .graphicsLayer {
                                                        scaleX = iconScale
                                                        scaleY = iconScale
                                                        transformOrigin = TransformOrigin(0.5f, 1f)
                                                        // Head center is 18dp up from tip. Scaling from the tip (1f) keeps this distance proportional.
                                                        translationY = -18.dp.toPx()
                                                    }
                                                    .background(Color.White, CircleShape)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.Red else Color.Gray.copy(alpha = 0.8f),
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .graphicsLayer { 
                                                        scaleX = iconScale
                                                        scaleY = iconScale
                                                        transformOrigin = TransformOrigin(0.5f, 1f)
                                                        // Move the icon center up by 12dp so the bottom tip is at the layout center (coordinate)
                                                        translationY = -12.dp.toPx()
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
