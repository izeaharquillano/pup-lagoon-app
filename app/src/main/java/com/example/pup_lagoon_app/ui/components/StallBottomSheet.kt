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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.pup_lagoon_app.SheetStage
import com.example.pup_lagoon_app.data.MergedRecords

@Composable
fun StallBottomSheetContent(
    stallName: String,
    stallId: String,
    foods: List<MergedRecords>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    stallImages: List<String> = emptyList(),
    sheetStage: SheetStage = SheetStage.Halfway
) {
    val isMinimized = sheetStage == SheetStage.Minimized

    val iconSize by animateDpAsState(
        targetValue = if (isMinimized) 24.dp else 40.dp,
        label = "iconSize"
    )

    val verticalPadding by animateDpAsState(
        targetValue = if (isMinimized) 4.dp else 8.dp,
        label = "verticalPadding"
    )

    val horizontalPadding by animateDpAsState(
        targetValue = if (isMinimized) 16.dp else 20.dp,
        label = "horizontalPadding"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // Stall Header - Now a Row to align the icon/name and dismiss button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = horizontalPadding,
                    vertical = verticalPadding
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Storefront,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(iconSize)
            )
            Spacer(modifier = Modifier.width(if (isMinimized) 8.dp else 12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedContent(
                        targetState = isMinimized,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "titleContent"
                    ) { minimized ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stallName,
                                style = if (minimized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            
                            if (minimized) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "#$stallId",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                // Expanded Stall ID (below name)
                AnimatedVisibility(
                    visible = !isMinimized,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Text(
                        text = "Stall #$stallId",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(if (isMinimized) 32.dp else 48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.Gray,
                    modifier = Modifier.size(if (isMinimized) 18.dp else 24.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = !isMinimized,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 16.dp)
                ) {
                    items(foods) { record ->
                        FoodItemCard(record = record, onClick = {})
                    }
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
        sheetStage = SheetStage.Halfway
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
        sheetStage = SheetStage.Minimized
    )
}
