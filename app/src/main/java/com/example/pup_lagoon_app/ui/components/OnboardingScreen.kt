package com.example.pup_lagoon_app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.util.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pup_lagoon_app.R
import com.example.pup_lagoon_app.ui.theme.Maroon
import com.example.pup_lagoon_app.ui.theme.PuplagoonappTheme
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector? = null,
    val imageRes: Int? = null
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val pages = listOf(
        OnboardingPage(
            title = "Welcome to PUPili!",
            description = "Your ultimate companion for finding the best food and stalls around the PUP Lagoon area.",
            imageRes = R.drawable.pupili_logo
        ),
        OnboardingPage(
            title = "Search & Filter",
            description = "Easily search for your favorite snacks or discover new ones using our smart filters for categories and price ranges.",
            icon = Icons.Default.Search
        ),
        OnboardingPage(
            title = "Interactive Map",
            description = "Tap on pins to see stall details, menus, and photos. No more getting lost while looking for lunch!",
            icon = Icons.Default.Map
        ),
        OnboardingPage(
            title = "Smart Navigation",
            description = "Select a gate and get step-by-step directions directly to your chosen stall.",
            icon = Icons.Default.Directions
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar (Back and Skip)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    IconButton(onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Gray)
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }

                TextButton(onClick = onComplete) {
                    Text("Skip", color = Color.Gray)
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { position ->
                OnboardingPageContent(
                    page = pages[position], 
                    pagerState = pagerState,
                    pageIndex = position
                )
            }

            // Bottom section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page Indicator
                Row {
                    repeat(pages.size) { index ->
                        val width = animateDpAsState(targetValue = if (pagerState.currentPage == index) 24.dp else 8.dp, label = "indicator_width")
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(if (pagerState.currentPage == index) Maroon else Color.LightGray)
                                .size(width = width.value, height = 8.dp)
                                .clickable {
                                    scope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                        )
                    }
                }

                // Next / Get Started button
                if (pagerState.currentPage == pages.size - 1) {
                    val scale by animateFloatAsState(
                        targetValue = 1.05f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "get_started_scale"
                    )
                    Button(
                        onClick = onComplete,
                        colors = ButtonDefaults.buttonColors(containerColor = Maroon),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .scale(scale)
                    ) {
                        Text("Get Started", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    IconButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Maroon)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(
    page: OnboardingPage, 
    pagerState: androidx.compose.foundation.pager.PagerState,
    pageIndex: Int
) {
    val pageOffset = (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .graphicsLayer {
                // Parallax effect
                translationX = pageOffset * size.width * 0.5f
                alpha = 1f - lerp(0f, 1f, kotlin.math.abs(pageOffset))
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = 1f - kotlin.math.abs(pageOffset) * 0.3f
                    scaleY = 1f - kotlin.math.abs(pageOffset) * 0.3f
                    rotationZ = pageOffset * 20f
                },
            contentAlignment = Alignment.Center
        ) {
            if (page.imageRes != null) {
                Image(
                    painter = painterResource(id = page.imageRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                )
            } else if (page.icon != null) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(Maroon.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = Maroon
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                translationY = pageOffset * 100f
            }
        ) {
            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Maroon,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.DarkGray,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    PuplagoonappTheme {
        OnboardingScreen(onComplete = {})
    }
}
