package com.example.pup_lagoon_app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.pup_lagoon_app.SheetStage
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import com.example.pup_lagoon_app.data.MergedRecords
import com.example.pup_lagoon_app.ui.utils.scrollbar

@Composable
fun StallBottomSheetContent(
    stallName: String,
    stallId: String,
    foods: List<MergedRecords>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    stallImages: List<String> = emptyList(),
    sheetStage: SheetStage = SheetStage.Halfway,
    progressProvider: () -> Float = { 0.5f } // 0f = Minimized, 0.5f = Halfway, 1f = Full
) {
    val density = LocalDensity.current
    val foodListState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // Stall Header - Fixed layout slots with fluid graphics layer transitions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp) 
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val progress = progressProvider()
            val normalizedProgress = (progress / 0.5f).coerceIn(0f, 1f)

            Icon(
                imageVector = Icons.Default.Storefront,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = normalizedProgress
                        val scale = 0.75f + (0.25f * normalizedProgress)
                        scaleX = scale * normalizedProgress
                        scaleY = scale * normalizedProgress
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                    .size(40.dp)
            )
            
            Spacer(
                modifier = Modifier
                    .width(12.dp)
                    .graphicsLayer {
                        alpha = normalizedProgress
                    }
            )
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        // Push up slightly as we expand to balance visual center with subtitle
                        val upwardShift = with(density) { 6.dp.toPx() }
                        translationY = -upwardShift * progress.coerceIn(0f, 1f)
                        
                        // Shift left to fill the space of the hidden icon when minimized
                        val shiftLeft = with(density) { 52.dp.toPx() } // 40dp (icon) + 12dp (spacer)
                        translationX = -shiftLeft * (1f - normalizedProgress)
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                // Name Row - Stays centered with the icon layout box
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.graphicsLayer {
                        val p = (progressProvider() / 0.5f).coerceIn(0f, 1f)
                        val scale = 0.85f + (0.15f * p)
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                ) {
                    Text(
                        text = stallName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.alignByBaseline()
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "#$stallId",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray,
                        modifier = Modifier
                            .alignByBaseline()
                            .graphicsLayer {
                                val p = progressProvider()
                                // Fade out the inline ID as we expand
                                alpha = (1f - (p / 0.2f)).coerceIn(0f, 1f)
                            }
                    )
                }

                // Subtitle ID - Sits below the name row, doesn't push it
                Text(
                    text = "Stall #$stallId",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.graphicsLayer {
                        val p = progressProvider()
                        alpha = ((p - 0.2f) / 0.3f).coerceIn(0f, 1f)
                        // Offset below the center (Title is at center)
                        translationY = with(density) { 18.dp.toPx() }
                    }
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.Gray,
                    modifier = Modifier.graphicsLayer {
                        val p = (progressProvider() / 0.5f).coerceIn(0f, 1f)
                        val scale = 0.8f + (0.2f * p)
                        scaleX = scale
                        scaleY = scale
                    }
                )
            }
        }

        // Main Content - Perfectly fluid alpha and translation
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .weight(1f)
                .graphicsLayer { 
                    val p = progressProvider()
                    alpha = ((p - 0.15f) / 0.35f).coerceIn(0f, 1f)
                    translationY = 30f * (1f - alpha)
                }
        ) {
            // Static content structure to prevent layout thrashing
            Spacer(modifier = Modifier.height(16.dp))

            // Stall Photos Section
            if (stallImages.isNotEmpty()) {
                Text(
                    text = "Stall Photos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow {
                    items(stallImages) { imageUrl ->
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageUrl)
                                    .crossfade(true)
                                    .build(),
                            contentDescription = "Stall Photo",
                            modifier = Modifier
                                    .size(120.dp)
                                    .padding(end = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.LightGray.copy(alpha = 0.3f)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Food List Section
            Text(
                text = "Available Foods",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                state = foodListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 16.dp)
                    .scrollbar(foodListState, autoHide = true)
            ) {
                items(
                    items = foods,
                    key = { it.id },
                    contentType = { "food_item" }
                ) { record ->
                    FoodItemCard(record = record, onClick = {})
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StallBottomSheetHalfwayPreview() {
    StallBottomSheetContent(
        stallName = "Sample Stall",
        stallId = "01",
        foods = emptyList(),
        onDismiss = {},
        stallImages = listOf("https://via.placeholder.com/150"),
        sheetStage = SheetStage.Halfway,
        progressProvider = { 0.5f }
    )
}

@Preview(showBackground = true)
@Composable
fun StallBottomSheetMinimizedPreview() {
    StallBottomSheetContent(
        stallName = "Sample Stall",
        stallId = "01",
        foods = emptyList(),
        onDismiss = {},
        stallImages = listOf("https://via.placeholder.com/150"),
        sheetStage = SheetStage.Minimized,
        progressProvider = { 0f }
    )
}
