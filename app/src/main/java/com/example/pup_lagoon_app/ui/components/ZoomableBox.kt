package com.example.pup_lagoon_app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoorSliding
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pup_lagoon_app.data.LabelType
import com.example.pup_lagoon_app.data.MapLabel
import com.example.pup_lagoon_app.data.StallLocation
import com.example.pup_lagoon_app.ui.theme.Maroon
import kotlinx.coroutines.launch
import kotlin.math.abs

private val PinShape = GenericShape { size, _ ->
    val scaleX = size.width / 24f
    val scaleY = size.height / 24f
    moveTo(12f * scaleX, 2f * scaleY)
    cubicTo(8.13f * scaleX, 2f * scaleY, 5f * scaleX, 5.13f * scaleY, 5f * scaleX, 9f * scaleY)
    cubicTo(5f * scaleX, 14.25f * scaleY, 12f * scaleX, 22f * scaleY, 12f * scaleX, 22f * scaleY)
    cubicTo(12f * scaleX, 22f * scaleY, 19f * scaleX, 16.75f * scaleY, 19f * scaleX, 9f * scaleY)
    cubicTo(19f * scaleX, 5.13f * scaleY, 15.87f * scaleX, 2f * scaleY, 12f * scaleX, 2f * scaleY)
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
    selectedStallLocations: Map<String, StallLocation> = emptyMap(),
    contentFullSize: IntSize? = null,
    keptPins: Map<String, StallLocation> = emptyMap(),
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
    allStallLocations: Map<String, StallLocation> = emptyMap(),
    isLoading: Boolean = false,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val scaleAnimatable = remember { Animatable(initialScale) }
    val offsetAnimatable = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var initialized by remember { mutableStateOf(false) }
    
    var showMapElements by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(600)
        showMapElements = true
    }

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
                return Offset(targetBaseX.coerceIn(-maxX, maxX), targetBaseY.coerceIn(-maxY, maxY))
            }

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
                                awaitFirstDown(requireUnconsumed = false)
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
                                        val relativeCentroid = Offset(centroid.x - baseWidthPx / 2f, centroid.y - baseHeightPx / 2f)
                                        val currentOffset = offsetAnimatable.value
                                        val newOffset = (currentOffset + relativeCentroid / oldScale) - (relativeCentroid / newScale + pan / oldScale)
                                        val currentScaleValue = scaleAnimatable.value
                                        val maxX = (baseWidthPx * currentScaleValue - containerWidthPx).coerceAtLeast(0f) / (2f * currentScaleValue)
                                        val maxY = (baseHeightPx * currentScaleValue - containerHeightPx).coerceAtLeast(0f) / (2f * currentScaleValue)
                                        val finalOffset = Offset(newOffset.x.coerceIn(-maxX, maxX), newOffset.y.coerceIn(-maxY, maxY))

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

                if (showMapElements && contentFullSize != null) {
                    val fullWidth = contentFullSize.width.toFloat()
                    val fullHeight = contentFullSize.height.toFloat()

                    // Render Route Guidance Path
                    if (navigationPath.isNotEmpty()) {
                        val normalizedPath = remember(navigationPath, fullWidth, fullHeight) {
                            navigationPath.map { point -> Offset(point.x / fullWidth, point.y / fullHeight) }
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

                    // Render All Stall Icons
                    allStallLocations.forEach { (id, stallLocation) ->
                        key(id) {
                            if (id !in selectedStallIds && id !in keptPins.keys) {
                                val iconX = (stallLocation.pixelX / fullWidth) * baseWidthPx
                                val iconY = (stallLocation.pixelY / fullHeight) * baseHeightPx
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
                                            val isVisible = abs(iconXRelCenter - off.x) < halfWidthVisible + margin &&
                                                            abs(iconYRelCenter - off.y) < halfHeightVisible + margin
                                            alpha = if (isVisible) 1f else 0f
                                            transformOrigin = TransformOrigin(0.5f, 1f)
                                            scaleX = 1f / s
                                            scaleY = 1f / s
                                            translationX = iconX - (140.dp.toPx() / 2f)
                                            translationY = iconY - (60.dp.toPx())
                                        }
                                        .size(width = 140.dp, height = 60.dp)
                                ) {
                                    // Full Icon (Visible when zoomed in)
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                alpha = if (currentScaleProvider() > 1.9f) 1f else 0f
                                            },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Bottom
                                    ) {
                                        Surface(
                                            color = Color.White.copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier
                                                .padding(bottom = 4.dp)
                                                .graphicsLayer { alpha = if (currentScaleProvider() > 2.6f) 1f else 0f }
                                                .shadow(2.dp, RoundedCornerShape(4.dp)),
                                            shadowElevation = 2.dp
                                        ) {
                                            val displayText = if (!stallLocation.stallName.isNullOrEmpty()) {
                                                val name = stallLocation.stallName
                                                if (name.length > 15) name.take(15) + "..." else name
                                            } else {
                                                "Stall ${id.trimStart('0')}"
                                            }
                                            Text(
                                                text = displayText,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.SemiBold),
                                                color = Maroon,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                maxLines = 1, softWrap = false, textAlign = TextAlign.Center
                                            )
                                        }
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .shadow(3.dp, CircleShape)
                                                .background(Maroon, CircleShape)
                                                .pointerInput(id) {
                                                    awaitEachGesture {
                                                        awaitFirstDown(requireUnconsumed = false)
                                                        val up = waitForUpOrCancellation()
                                                        if (up != null && currentScaleProvider() > 1.9f) {
                                                            up.consume()
                                                            onPinClick?.invoke(id)
                                                        }
                                                    }
                                                }
                                        ) {
                                            Icon(imageVector = Icons.Default.Storefront, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    // Simple Dot (Visible when zoomed out)
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .graphicsLayer {
                                                val s = currentScaleProvider()
                                                alpha = if (s <= 1.9f) 1f else 0f
                                                val dotScale = (1.5f / s).coerceIn(1f, 2f)
                                                scaleX = dotScale
                                                scaleY = dotScale
                                            }
                                            .size(8.dp)
                                            .background(Maroon, CircleShape)
                                            .pointerInput(id) {
                                                awaitEachGesture {
                                                    awaitFirstDown(requireUnconsumed = false)
                                                    val up = waitForUpOrCancellation()
                                                    if (up != null && currentScaleProvider() <= 1.9f) {
                                                        up.consume()
                                                        onPinClick?.invoke(id)
                                                    }
                                                }
                                            }
                                    )
                                }
                            }
                        }
                    }

                    // Render all kept pins
                    keptPins.forEach { (id, stallLocation) ->
                        key(id) {
                            if (id !in selectedStallIds) {
                                val pinX = (stallLocation.pixelX / fullWidth) * baseWidthPx
                                val pinY = (stallLocation.pixelY / fullHeight) * baseHeightPx
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
                                                awaitFirstDown(requireUnconsumed = false)
                                                val up = waitForUpOrCancellation()
                                                if (up != null) {
                                                    val s = currentScaleProvider()
                                                    val off = currentOffsetProvider()
                                                    val isVisible = abs(xRel - off.x) < (containerWidthPx / (2f * s)) + (60f * density / s) &&
                                                                    abs(yRel - off.y) < (containerHeightPx / (2f * s)) + (60f * density / s)
                                                    if (isVisible) {
                                                        up.consume()
                                                        onPinClick?.invoke(id)
                                                    }
                                                }
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
                                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = Maroon, modifier = Modifier.fillMaxSize().shadow(4.dp, PinShape))
                                        Box(modifier = Modifier.size(10.dp).graphicsLayer { translationY = -5.dp.toPx() }.background(Color.White, CircleShape))
                                    }
                                }
                            }
                        }
                    }

                    // Selected Pins Overlay
                    selectedStallLocations.forEach { (id, stallLocation) ->
                        key(id) {
                            val pinX = (stallLocation.pixelX / fullWidth) * baseWidthPx
                            val pinY = (stallLocation.pixelY / fullHeight) * baseHeightPx
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.graphicsLayer {
                                    val s = currentScaleProvider()
                                    transformOrigin = TransformOrigin(0.5f, 1f)
                                    scaleX = 1f / s
                                    scaleY = 1f / s
                                    translationX = pinX - 27.dp.toPx()
                                    translationY = pinY - 54.dp.toPx()
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(54.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn, contentDescription = null, tint = Color.Red,
                                        modifier = Modifier.fillMaxSize().shadow(6.dp, PinShape).pointerInput(id) {
                                            awaitEachGesture {
                                                awaitFirstDown(requireUnconsumed = false)
                                                val up = waitForUpOrCancellation()
                                                if (up != null) { up.consume(); onPinClick?.invoke(id) }
                                            }
                                        }
                                    )
                                    Box(modifier = Modifier.size(12.dp).graphicsLayer { translationY = -6.2.dp.toPx() }.background(Color.White, CircleShape))
                                }
                            }
                        }
                    }

                    // Render Map Labels
                    mapLabels.forEach { label ->
                        key(label.id) {
                            val labelX = (label.pixelX / fullWidth) * baseWidthPx
                            val labelY = (label.pixelY / fullHeight) * baseHeightPx
                            val xRel = labelX - baseWidthPx / 2f
                            val yRel = labelY - baseHeightPx / 2f
                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        val s = currentScaleProvider()
                                        val off = currentOffsetProvider()
                                        val isVisible = abs(xRel - off.x) < (containerWidthPx / (2f * s)) + (100f * density / s) &&
                                                        abs(yRel - off.y) < (containerHeightPx / (2f * s)) + (100f * density / s)
                                        alpha = if (isVisible) 1f else 0f
                                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                                        scaleX = 1f / s
                                        scaleY = 1f / s
                                        translationX = labelX - (100.dp.toPx() / 2f)
                                        translationY = labelY - (50.dp.toPx() / 2f)
                                    }
                                    .pointerInput(label.id) {
                                        awaitEachGesture {
                                            awaitFirstDown(requireUnconsumed = false)
                                            val up = waitForUpOrCancellation()
                                            if (up != null && label.type == LabelType.LANDMARK) {
                                                val s = currentScaleProvider()
                                                val off = currentOffsetProvider()
                                                val isVisible = abs(xRel - off.x) < (containerWidthPx / (2f * s)) + (100f * density / s) &&
                                                                abs(yRel - off.y) < (containerHeightPx / (2f * s)) + (100f * density / s)
                                                if (isVisible) { up.consume(); onLandmarkClick?.invoke(label.id) }
                                            }
                                        }
                                    }
                                    .size(width = 100.dp, height = 50.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                when (label.type) {
                                    LabelType.BUILDING -> {
                                        Surface(color = Color.White.copy(alpha = 0.8f), shape = RoundedCornerShape(4.dp), shadowElevation = 2.dp) {
                                            Text(text = label.text, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), color = Color.Black)
                                        }
                                    }
                                    LabelType.STREET -> {
                                        Text(text = label.text, style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic, fontSize = 8.sp, fontWeight = FontWeight.Medium), color = Color.DarkGray.copy(alpha = 0.7f), modifier = Modifier.rotate(label.rotation))
                                    }
                                    LabelType.LANDMARK -> {
                                        val isSelected = label.id == selectedGateId
                                        val iconScale = if (isSelected) 1.5f else 1.0f
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.onGloballyPositioned { coords -> if (label.text == "Gate 1") onGate1Positioned?.invoke(coords.boundsInRoot()) }
                                                .pointerInput(label.id) {
                                                    awaitEachGesture {
                                                        awaitFirstDown(requireUnconsumed = false)
                                                        val up = waitForUpOrCancellation()
                                                        if (up != null) { up.consume(); onLandmarkClick?.invoke(label.id) }
                                                    }
                                                }
                                        ) {
                                            Surface(
                                                color = Color.White.copy(alpha = 0.8f), shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.graphicsLayer { translationY = if (isSelected) -30.dp.toPx() else -22.dp.toPx() }
                                            ) {
                                                Text(text = label.text, style = MaterialTheme.typography.labelSmall.copy(fontSize = if (isSelected) 10.sp else 9.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold), color = if (isSelected) Color.Red else Color.Gray, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                            }
                                            Box(contentAlignment = Alignment.Center) {
                                                Box(modifier = Modifier.size(16.dp).graphicsLayer { scaleX = iconScale; scaleY = iconScale }.background(Color.White, RoundedCornerShape(4.dp)))
                                                Icon(imageVector = Icons.Default.DoorSliding, contentDescription = null, tint = if (isSelected) Color.Red else Color.Gray.copy(alpha = 0.8f), modifier = Modifier.size(24.dp).graphicsLayer { scaleX = iconScale; scaleY = iconScale })
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
}
