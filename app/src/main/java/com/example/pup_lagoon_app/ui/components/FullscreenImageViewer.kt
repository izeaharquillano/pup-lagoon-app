package com.example.pup_lagoon_app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch

@Composable
fun FullscreenImageViewer(
    images: List<String>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        BackHandler {
            onDismiss()
        }
    }

    val pagerState = rememberPagerState(
        initialPage = currentIndex,
        pageCount = { images.size }
    )
    val scope = rememberCoroutineScope()
    var isZoomed by remember { mutableStateOf(false) }

    // Sync external index changes to pager
    LaunchedEffect(currentIndex) {
        if (pagerState.currentPage != currentIndex) {
            pagerState.scrollToPage(currentIndex)
        }
    }

    // Sync pager changes back to external state
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onIndexChange(page)
            isZoomed = false // Reset zoom on page change
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures { /* consume to prevent passthrough */ }
                }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
                userScrollEnabled = !isZoomed // Disable swiping when zoomed
            ) { page ->
                val imageUrl = images.getOrNull(page)
                if (imageUrl != null) {
                    ZoomableImage(
                        imageUrl = imageUrl,
                        onZoomChange = { zoomed -> isZoomed = zoomed }
                    )
                }
            }

            // Close Button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Fullscreen",
                    tint = Color.White
                )
            }

            // Navigation Buttons
            if (images.size > 1 && !isZoomed) {
                // Previous Button
                if (pagerState.currentPage > 0) {
                    IconButton(
                        onClick = { 
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 16.dp)
                            .background(Color.Black.copy(alpha = 0.3f), shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Image",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Next Button
                if (pagerState.currentPage < images.size - 1) {
                    IconButton(
                        onClick = { 
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                            .background(Color.Black.copy(alpha = 0.3f), shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Image",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ZoomableImage(
    imageUrl: String,
    onZoomChange: (Boolean) -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    do {
                        val event = awaitPointerEvent()
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()

                        // A gesture is considered a zoom/pan for this component if:
                        // 1. We are already zoomed in (scale > 1)
                        // 2. Or the current gesture is attempting to zoom (zoom != 1)
                        if (scale > 1f || zoom != 1f) {
                            val nextScale = (scale * zoom).coerceIn(1f, 4f)
                            
                            if (nextScale <= 1.05f) {
                                scale = 1f
                                offset = androidx.compose.ui.geometry.Offset.Zero
                                onZoomChange(false)
                            } else {
                                scale = nextScale
                                offset += pan
                                onZoomChange(true)
                            }
                            
                            // Consume the changes so the parent (Pager) doesn't receive them
                            event.changes.forEach { change ->
                                change.consume()
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(Unit) {
                // Also ensure a single tap or long press resets zoom if needed, 
                // but primarily we want to make sure it doesn't stay stuck.
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = androidx.compose.ui.geometry.Offset.Zero
                            onZoomChange(false)
                        } else {
                            scale = 2.5f
                            onZoomChange(true)
                        }
                    }
                )
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Fullscreen Stall Photo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}
