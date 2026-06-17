package com.example.pup_lagoon_app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.DoorSliding
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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
import androidx.compose.ui.draw.shadow
import com.example.pup_lagoon_app.ui.theme.Maroon

private val PinShape = GenericShape { size, _ ->
    // Exact Material Design Path for LocationOn (24x24 grid)
    // M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z
    
    val scaleX = size.width / 24f
    val scaleY = size.height / 24f
    
    // Scale everything to fit the component size
    moveTo(12f * scaleX, 2f * scaleY)
    
    // C8.13 2 5 5.13 5 9
    cubicTo(
        8.13f * scaleX, 2f * scaleY,
        5f * scaleX, 5.13f * scaleY,
        5f * scaleX, 9f * scaleY
    )
    
    // c0 5.25 7 13 7 13
    // Note: relative cubicTo or lineTo?
    // The "s7-7.75 7-13" implies a symmetric curve.
    // Let's use the explicit points from the full path data:
    // M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z
    
    // From (5, 9) to bottom tip (12, 22)
    cubicTo(
        5f * scaleX, 14.25f * scaleY,
        12f * scaleX, 22f * scaleY,
        12f * scaleX, 22f * scaleY
    )
    
    // From bottom tip (12, 22) to right side (19, 9)
    // s7-7.75 7-13 -> relative to current (12, 22) -> (19, 14.25) to (19, 9)
    cubicTo(
        12f * scaleX, 22f * scaleY,
        19f * scaleX, 16.75f * scaleY,
        19f * scaleX, 9f * scaleY
    )
    
    // From (19, 9) back to top (12, 2)
    cubicTo(
        19f * scaleX, 5.13f * scaleY,
        15.87f * scaleX, 2f * scaleY,
        12f * scaleX, 2f * scaleY
    )

    close()
}

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
    onGate1Positioned: ((Rect) -> Unit)? = null,
    onPan: (() -> Unit)? = null,
    onZoom: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onInteraction: (() -> Unit)? = null,
    allStallLocations: Map<String, Offset> = emptyMap(),
    isLoading: Boolean = false,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val scaleAnimatable = remember { Animatable(initialScale) }
    val offsetAnimatable = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var initialized by remember { mutableStateOf(false) }
    
    // Staggered initialization to prevent startup choppiness
    var showMapElements by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // Delay map elements slightly to let the initial layout and map image stabilize
        kotlinx.coroutines.delay(600)
        showMapElements = true
    }

    // Use lambda providers for state reads to defer to draw phase and avoid recomposition
    val currentOffsetProvider = { offsetAnimatable.value }
    val currentScaleProvider = { scaleAnimatable.value }

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

            val baseWidthPx = baseWidth * density
            val baseHeightPx = baseHeight * density
            val containerWidthPx = containerWidth * density
            val containerHeightPx = containerHeight * density

            // Calculation helper
            fun calculateBoundOffset(targetPixel: Offset, fullSize: IntSize): Offset {
                val fullWidth = fullSize.width.toFloat()
                val fullHeight = fullSize.height.toFloat()

                val normalizedX = targetPixel.x / fullWidth
                val normalizedY = targetPixel.y / fullHeight

                val targetBaseX = (normalizedX - 0.5f) * baseWidthPx
                val targetBaseY = (normalizedY - 0.5f) * baseHeightPx

                val currentScale = scaleAnimatable.value
                val maxX = (baseWidthPx * currentScale - containerWidthPx).coerceAtLeast(0f) / (2f * currentScale)
                val maxY = (baseHeightPx * currentScale - containerHeightPx).coerceAtLeast(0f) / (2f * currentScale)

                return Offset(
                    targetBaseX.coerceIn(-maxX, maxX),
                    targetBaseY.coerceIn(-maxY, maxY)
                )
            }

            // Initial positioning
            LaunchedEffect(containerWidth, containerHeight, contentFullSize, initialCenterPixel) {
                if (!initialized && contentFullSize != null && initialCenterPixel != null) {
                    val initialOffset = calculateBoundOffset(initialCenterPixel, contentFullSize)
                    offsetAnimatable.snapTo(initialOffset)
                    initialized = true
                }
            }

            LaunchedEffect(targetCenterPixel, contentFullSize) {
                if (targetCenterPixel != null && contentFullSize != null) {
                    val targetOffset = calculateBoundOffset(targetCenterPixel, contentFullSize)
                    scope.launch {
                        offsetAnimatable.animateTo(
                            targetValue = targetOffset,
                            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .requiredSize(width = baseWidth.dp, height = baseHeight.dp)
                    .pointerInput(isLoading) {
                        if (!isLoading) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val up = waitForUpOrCancellation()
                                if (up != null) {
                                    up.consume()
                                    onClick?.invoke()
                                }
                            }
                        }
                    }
                    .pointerInput(isLoading) {
                        if (!isLoading) {
                            awaitEachGesture {
                                awaitFirstDown()
                                onInteraction?.invoke()
                                do {
                                    val event = awaitPointerEvent()
                                    val zoom = event.calculateZoom()
                                    val pan = event.calculatePan()
                                    val centroid = event.calculateCentroid()

                                    if (zoom != 1f || pan != Offset.Zero) {
                                        if (zoom != 1f) onZoom?.invoke()
                                        if (pan != Offset.Zero) onPan?.invoke()

                                        val oldScale = scaleAnimatable.value
                                        val newScale = (oldScale * zoom).coerceIn(minScale, maxScale)
                                        
                                        val relativeCentroid = Offset(
                                            centroid.x - baseWidthPx / 2f,
                                            centroid.y - baseHeightPx / 2f
                                        )

                                        val currentOffset = offsetAnimatable.value
                                        val newOffset = (currentOffset + relativeCentroid / oldScale) - 
                                                    (relativeCentroid / newScale + pan / oldScale)

                                        val currentScaleValue = scaleAnimatable.value
                                        val maxX = (baseWidthPx * currentScaleValue - containerWidthPx).coerceAtLeast(0f) / (2f * currentScaleValue)
                                        val maxY = (baseHeightPx * currentScaleValue - containerHeightPx).coerceAtLeast(0f) / (2f * currentScaleValue)

                                        val finalOffset = Offset(
                                            newOffset.x.coerceIn(-maxX, maxX),
                                            newOffset.y.coerceIn(-maxY, maxY)
                                        )

                                        // Update animatables using the composition's scope
                                        // but launch it as UNDISPATCHED to handle the update immediately
                                        // without the 1-frame/dispatcher delay
                                        scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                                            scaleAnimatable.snapTo(newScale)
                                            offsetAnimatable.snapTo(finalOffset)
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                    }
                    .graphicsLayer {
                        val s = currentScaleProvider()
                        val off = currentOffsetProvider()
                        scaleX = s
                        scaleY = s
                        translationX = -off.x * s
                        translationY = -off.y * s
                    }
            ) {
                content()

                // Render Route Guidance Path
                if (showMapElements && contentFullSize != null && navigationPath.isNotEmpty()) {
                    val fullWidth = contentFullSize.width.toFloat()
                    val fullHeight = contentFullSize.height.toFloat()

                    // Pre-map the raw normalized points to avoid allocation in draw phase
                    val normalizedPath = remember(navigationPath, fullWidth, fullHeight) {
                        navigationPath.map { point ->
                            Offset(point.x / fullWidth, point.y / fullHeight)
                        }
                    }

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val s = currentScaleProvider()
                        
                        val path = androidx.compose.ui.graphics.Path()
                        normalizedPath.forEachIndexed { i, point ->
                            val px = point.x * baseWidthPx
                            val py = point.y * baseHeightPx - (2.dp.toPx() / s)
                            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                        }

                        if (normalizedPath.size >= 2) {
                            drawPath(
                                path = path,
                                color = Color.Red,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 3.dp.toPx() / s,
                                    cap = StrokeCap.Round,
                                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                                )
                            )
                        }
                    }
                }

                // Render All Stall Icons (Storefront icon, clickable at higher zoom)
                if (showMapElements && contentFullSize != null) {
                    val fullWidth = contentFullSize.width.toFloat()
                    val fullHeight = contentFullSize.height.toFloat()

                    allStallLocations.forEach { (id, location) ->
                        // Only show if NOT currently selected or kept (to avoid overlap with pins)
                        if (id !in selectedStallIds && id !in keptPins.keys) {
                            val iconX = (location.x / fullWidth) * baseWidthPx
                            val iconY = (location.y / fullHeight) * baseHeightPx

                            val iconXRelCenter = iconX - baseWidthPx / 2f
                            val iconYRelCenter = iconY - baseHeightPx / 2f

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .graphicsLayer {
                                        val s = currentScaleProvider()
                                        val off = currentOffsetProvider()
                                        
                                        val halfWidthVisible = containerWidthPx / (2f * s)
                                        val halfHeightVisible = containerHeightPx / (2f * s)
                                        val margin = 50f * density / s

                                        // Progressive Disclosure:
                                        // Dots are always "present", full icons show after 2.1f
                                        val isVisible = abs(iconXRelCenter - off.x) < halfWidthVisible + margin &&
                                                        abs(iconYRelCenter - off.y) < halfHeightVisible + margin
                                        
                                        alpha = if (isVisible) 1f else 0f
                                        transformOrigin = TransformOrigin(0.5f, 1f)
                                        scaleX = 1f / s
                                        scaleY = 1f / s
                                        translationX = iconX - (40.dp.toPx() / 2f)
                                        
                                        // The container is 60dp high.
                                        // The circle icon is 24dp high and at the BOTTOM of the container.
                                        // The center of the circle is at 12dp from the bottom.
                                        // To center that circle on 'iconY', we need to offset by (60 - 12) = 48dp.
                                        translationY = iconY - (60.dp.toPx())
                                    }
                                    .size(width = 40.dp, height = 60.dp)
                                    .pointerInput(id) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            val up = waitForUpOrCancellation()
                                            if (up != null) {
                                                val s = currentScaleProvider()
                                                val off = currentOffsetProvider()
                                                val halfWidthVisible = containerWidthPx / (2f * s)
                                                val halfHeightVisible = containerHeightPx / (2f * s)
                                                val margin = 50f * density / s

                                                val isVisible = abs(iconXRelCenter - off.x) < halfWidthVisible + margin &&
                                                                abs(iconYRelCenter - off.y) < halfHeightVisible + margin

                                                // Only allow click if zoomed in enough to see what it is
                                                if (isVisible && s > 2.0f) {
                                                    up.consume()
                                                    onPinClick?.invoke(id)
                                                }
                                            }
                                        }
                                    }
                            ) {
                                // Full Icon (Visible when zoomed in)
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            alpha = if (currentScaleProvider() > 2.1f) 1f else 0f
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    // Stall Number
                                    Surface(
                                        color = Color.White.copy(alpha = 0.8f),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier
                                            .padding(bottom = 4.dp)
                                            .shadow(2.dp, RoundedCornerShape(4.dp)),
                                        shadowElevation = 2.dp
                                    ) {
                                        Text(
                                            text = "Stall ${id.trimStart('0')}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = Maroon,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }

                                    // Storefront Icon with background circle
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .shadow(3.dp, CircleShape)
                                            .background(Maroon, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Storefront,
                                            contentDescription = "Stall Icon",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // Simple Dot (Visible when zoomed out)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .graphicsLayer {
                                            val s = currentScaleProvider()
                                            alpha = if (s <= 2.1f) 1f else 0f
                                            
                                            val dotScale = (1.5f / s).coerceIn(1f, 2f)
                                            scaleX = dotScale
                                            scaleY = dotScale
                                        }
                                        .size(8.dp)
                                        .background(Maroon, CircleShape)
                                )
                            }
                        }
                    }
                }

                // Render all kept pins
                if (showMapElements && contentFullSize != null) {
                    val fullWidth = contentFullSize.width.toFloat()
                    val fullHeight = contentFullSize.height.toFloat()
                    
                    keptPins.forEach { (id, location) ->
                        // Skip if it's one of the currently selected stalls
                        if (id !in selectedStallIds) {
                            val pinX = (location.x / fullWidth) * baseWidthPx
                            val pinY = (location.y / fullHeight) * baseHeightPx

                            val xRel = pinX - baseWidthPx / 2f
                            val yRel = pinY - baseHeightPx / 2f
                            
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .graphicsLayer {
                                        val s = currentScaleProvider()
                                        val off = currentOffsetProvider()
                                        
                                        val halfWidthVisible = containerWidthPx / (2f * s)
                                        val halfHeightVisible = containerHeightPx / (2f * s)
                                        val margin = 60f * density / s

                                        val isVisible = abs(xRel - off.x) < halfWidthVisible + margin &&
                                                        abs(yRel - off.y) < halfHeightVisible + margin

                                        alpha = if (isVisible) 1f else 0f
                                        transformOrigin = TransformOrigin(0.5f, 1f)
                                        scaleX = 1f / s
                                        scaleY = 1f / s
                                        translationX = pinX - 22.dp.toPx()
                                        translationY = pinY - 44.dp.toPx()
                                    }
                                    .pointerInput(id) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            val up = waitForUpOrCancellation()
                                            if (up != null) {
                                                val s = currentScaleProvider()
                                                val off = currentOffsetProvider()
                                                val halfWidthVisible = containerWidthPx / (2f * s)
                                                val halfHeightVisible = containerHeightPx / (2f * s)
                                                val margin = 60f * density / s
                                                val isVisible = abs(xRel - off.x) < halfWidthVisible + margin &&
                                                                abs(yRel - off.y) < halfHeightVisible + margin
                                                
                                                if (isVisible) {
                                                    up.consume()
                                                    onPinClick?.invoke(id)
                                                }
                                            }
                                        }
                                    }
                            ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(44.dp)
                            ) {
                                // 1. The Pin Icon with Shadow
                                // No background here to prevent side overflow
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Kept Pin",
                                    tint = Maroon,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .shadow(elevation = 4.dp, shape = PinShape)
                                )

                                // 2. The White Filler for the hole (Placed on top)
                                // Material 24x24 grid: hole center (12, 9.5), radius 2.5
                                // 44dp scale: hole size ~9.17dp, offset from center -4.58dp
                                // Adjusted: Moving up slightly more (from -4.58 to -5.0) to fix sliver
                                Box(
                                    modifier = Modifier
                                        .size(10.dp) // Slightly larger than hole (9.17dp) to ensure coverage
                                        .graphicsLayer {
                                            translationY = -5.0.dp.toPx()
                                        }
                                        .background(Color.White, CircleShape)
                                )
                            }
                            }
                        }
                    }
                }

                // Selected Pins Overlay
                if (showMapElements && contentFullSize != null) {
                    val fullWidth = contentFullSize.width.toFloat()
                    val fullHeight = contentFullSize.height.toFloat()

                    selectedStallLocations.forEach { (id, location) ->
                        val pinX = (location.x / fullWidth) * baseWidthPx
                        val pinY = (location.y / fullHeight) * baseHeightPx

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .graphicsLayer {
                                    val s = currentScaleProvider()
                                    transformOrigin = TransformOrigin(0.5f, 1f)
                                    scaleX = 1f / s
                                    scaleY = 1f / s
                                    translationX = pinX - 27.dp.toPx()
                                    translationY = pinY - 54.dp.toPx()
                                }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(54.dp)
                            ) {
                                // 1. The Pin Icon with Shadow
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Selected Stall Pin",
                                    tint = Color.Red,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .shadow(elevation = 6.dp, shape = PinShape)
                                        .pointerInput(id) {
                                            awaitEachGesture {
                                                val down = awaitFirstDown(requireUnconsumed = false)
                                                val up = waitForUpOrCancellation()
                                                if (up != null) {
                                                    up.consume()
                                                    onPinClick?.invoke(id)
                                                }
                                            }
                                        }
                                )

                                // 2. The White Filler for the hole
                                // 54dp scale: hole size ~11.25dp, offset from center -5.625dp
                                // Adjusted: Moving up slightly more (from -5.625 to -6.2) to fix sliver
                                Box(
                                    modifier = Modifier
                                        .size(12.dp) // Slightly larger than hole (11.25dp)
                                        .graphicsLayer {
                                            translationY = -6.2.dp.toPx()
                                        }
                                        .background(Color.White, CircleShape)
                                )
                            }
                        }
                    }

                    // Render Map Labels
                    mapLabels.forEach { label ->
                        val labelX = (label.pixelX / fullWidth) * baseWidthPx
                        val labelY = (label.pixelY / fullHeight) * baseHeightPx

                        val xRel = labelX - baseWidthPx / 2f
                        val yRel = labelY - baseHeightPx / 2f
                        
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    val s = currentScaleProvider()
                                    val off = currentOffsetProvider()

                                    val halfWidthVisible = containerWidthPx / (2f * s)
                                    val halfHeightVisible = containerHeightPx / (2f * s)
                                    val margin = 100f * density / s

                                    val isVisible = abs(xRel - off.x) < halfWidthVisible + margin &&
                                                    abs(yRel - off.y) < halfHeightVisible + margin

                                    alpha = if (isVisible) 1f else 0f
                                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                                    scaleX = 1f / s
                                    scaleY = 1f / s
                                    translationX = labelX - (100.dp.toPx() / 2f)
                                    translationY = labelY - (50.dp.toPx() / 2f)
                                }
                                .pointerInput(label.id) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val up = waitForUpOrCancellation()
                                        if (up != null) {
                                            if (label.type == LabelType.LANDMARK) {
                                                val s = currentScaleProvider()
                                                val off = currentOffsetProvider()
                                                val halfWidthVisible = containerWidthPx / (2f * s)
                                                val halfHeightVisible = containerHeightPx / (2f * s)
                                                val margin = 100f * density / s
                                                val isVisible = abs(xRel - off.x) < halfWidthVisible + margin &&
                                                                abs(yRel - off.y) < halfHeightVisible + margin
                                                
                                                if (isVisible) {
                                                    up.consume()
                                                    onLandmarkClick?.invoke(label.id)
                                                }
                                            }
                                        }
                                    }
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
                                                .onGloballyPositioned { coords ->
                                                    if (label.text == "Gate 1") {
                                                        onGate1Positioned?.invoke(coords.boundsInRoot())
                                                    }
                                                }
                                                .pointerInput(label.id) {
                                                    awaitEachGesture {
                                                        val down = awaitFirstDown(requireUnconsumed = false)
                                                        val up = waitForUpOrCancellation()
                                                        if (up != null) {
                                                            up.consume()
                                                            onLandmarkClick?.invoke(label.id)
                                                        }
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
                                                        // Base: Adjusted to -22dp for consistency with stall icons
                                                        // Selection: Adjusted to -30dp to clear expanded icon while maintaining close spacing
                                                        translationY = if (isSelected) -30.dp.toPx() else -22.dp.toPx()
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
                                            // White square background for the icon
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .graphicsLayer {
                                                        scaleX = iconScale
                                                        scaleY = iconScale
                                                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                                                    }
                                                    .background(Color.White, RoundedCornerShape(4.dp))
                                            )
                                            Icon(
                                                imageVector = Icons.Default.DoorSliding,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.Red else Color.Gray.copy(alpha = 0.8f),
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .graphicsLayer { 
                                                        scaleX = iconScale
                                                        scaleY = iconScale
                                                        transformOrigin = TransformOrigin(0.5f, 0.5f)
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
