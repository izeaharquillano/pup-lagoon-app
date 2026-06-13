package com.example.pup_lagoon_app.ui.utils

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.scrollbar(
    state: LazyListState,
    thickness: Dp = 4.dp,
    color: Color = Color.Gray.copy(alpha = 0.5f),
    autoHide: Boolean = true
): Modifier {
    val isScrollInProgress by remember { derivedStateOf { state.isScrollInProgress } }
    val alpha by animateFloatAsState(
        targetValue = if (!autoHide || isScrollInProgress) 1f else 0f,
        animationSpec = tween(durationMillis = if (isScrollInProgress) 0 else 500),
        label = "scrollbar_alpha"
    )

    return this.drawWithContent {
        drawContent()
        if (alpha > 0f) {
            val layoutInfo = state.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) return@drawWithContent

            val totalItemsCount = layoutInfo.totalItemsCount
            val visibleItemsCount = visibleItemsInfo.size

            if (visibleItemsCount < totalItemsCount) {
                val viewportSize = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                val avgItemSize = visibleItemsInfo.sumOf { it.size }.toFloat() / visibleItemsCount
                val totalSize = avgItemSize * totalItemsCount
                val scrollOffset = visibleItemsInfo.first().index * avgItemSize - visibleItemsInfo.first().offset

                val knobHeight = (viewportSize / totalSize) * viewportSize
                val knobTop = (scrollOffset / totalSize) * viewportSize

                drawRect(
                    color = color.copy(alpha = color.alpha * alpha),
                    topLeft = Offset(size.width - thickness.toPx(), knobTop),
                    size = Size(thickness.toPx(), knobHeight)
                )
            }
        }
    }
}
