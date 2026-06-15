package com.example.pup_lagoon_app

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.pup_lagoon_app.ui.components.StallBottomSheetContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pup_lagoon_app.data.FoodRepository
import com.example.pup_lagoon_app.data.MergedRecords
import com.example.pup_lagoon_app.ui.components.FilterDialog
import com.example.pup_lagoon_app.ui.components.FoodItemCard
import com.example.pup_lagoon_app.ui.components.ZoomableBox
import com.example.pup_lagoon_app.ui.theme.Maroon
import com.example.pup_lagoon_app.ui.theme.PuplagoonappTheme
import com.example.pup_lagoon_app.ui.utils.scrollbar
import com.example.pup_lagoon_app.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PuplagoonappTheme {
                MainScreen()
            }
        }
    }
}

enum class SheetStage { Minimized, Halfway, Full }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModelOverride: MainViewModel? = null) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val repository = remember { FoodRepository(context) }
    val viewModel: MainViewModel = viewModelOverride ?: viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(repository) as T
            }
        }
    )

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val selectedCategories by viewModel.selectedCategories.collectAsState()
    val minPrice by viewModel.minPrice.collectAsState()
    val maxPrice by viewModel.maxPrice.collectAsState()

    if (viewModel.showFilterDialog) {
        FilterDialog(
            categories = viewModel.getAllCategories(),
            selectedCategories = selectedCategories,
            minPrice = minPrice,
            maxPrice = maxPrice,
            onDismiss = { viewModel.toggleFilterDialog() },
            onApply = { categories, min, max ->
                viewModel.onApplyFilters(categories, min, max)
            }
        )
    }

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val screenHeight = constraints.maxHeight.toFloat()
            
            // Anchors for the 3-stage bottom sheet
            // Minimized: ~80dp from bottom (Handle + Header)
            // Halfway: 53% of screen height from top (47% from bottom)
            val minimizedOffset = screenHeight - with(density) { 100.dp.toPx() }
            val halfwayOffset = screenHeight * 0.53f
            val fullOffset = with(density) { 40.dp.toPx() }

            val anchors = remember(screenHeight) {
                DraggableAnchors {
                    SheetStage.Minimized at minimizedOffset
                    SheetStage.Halfway at halfwayOffset
                    SheetStage.Full at fullOffset
                }
            }

            val snapAnimationSpec = spring<Float>(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = 3000f // Balanced stiffness for snappy yet smooth feel
            )

            val anchoredDraggableState = remember(anchors) {
                AnchoredDraggableState(
                    initialValue = SheetStage.Minimized,
                    anchors = anchors
                )
            }

            val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
                state = anchoredDraggableState,
                positionalThreshold = { distance: Float -> distance * 0.4f },
                animationSpec = snapAnimationSpec
            )

            // Lambda provider for progress to avoid recomposition
            val sheetProgressProvider = remember(anchoredDraggableState, minimizedOffset, fullOffset) {
                {
                    val offset = try { anchoredDraggableState.requireOffset() } catch (e: Exception) { minimizedOffset }
                    val totalRange = minimizedOffset - fullOffset
                    if (totalRange > 0) {
                        ((minimizedOffset - offset) / totalRange).coerceIn(0f, 1f)
                    } else 0f
                }
            }

            // Reset to Halfway when a new stall is selected
            LaunchedEffect(viewModel.selectedStallId) {
                if (viewModel.selectedStallId != null) {
                    anchoredDraggableState.animateTo(SheetStage.Halfway)
                }
            }

            // LAYER 1: Background Map Image
            val mapPainter = painterResource(id = R.drawable.university_map)
            val mapSize = mapPainter.intrinsicSize
            
            ZoomableBox(
                modifier = Modifier.fillMaxSize(),
                contentAspectRatio = if (mapSize.width > 0) mapSize.width / mapSize.height else 1f,
                initialCenterPixel = Offset(1818f, 1281f),
                targetCenterPixel = viewModel.selectedStallLocation,
                selectedStallIds = viewModel.selectedStallIds,
                selectedStallLocations = viewModel.selectedStallLocations,
                contentFullSize = IntSize(mapSize.width.toInt(), mapSize.height.toInt()),
                keptPins = viewModel.keptStallLocations,
                onPinClick = { stallId ->
                    viewModel.selectStallById(stallId)
                },
                onInteraction = {
                    if (anchoredDraggableState.currentValue != SheetStage.Minimized) {
                        scope.launch {
                            anchoredDraggableState.animateTo(SheetStage.Minimized)
                        }
                    }
                },
                onClick = {
                    if (anchoredDraggableState.currentValue != SheetStage.Minimized) {
                        scope.launch {
                            anchoredDraggableState.animateTo(SheetStage.Minimized)
                        }
                    }
                }
            ) {
                Image(
                    painter = mapPainter,
                    contentDescription = "University Map",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                    alpha = 1.0f
                )
            }

            // LAYER 2: UI Search Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, top = 40.dp, bottom = 0.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val hasActiveFilterOrSearch by remember {
                    derivedStateOf {
                        viewModel.showResults && (
                            searchResults.isNotEmpty() || 
                            (searchQuery.length >= 2) || 
                            selectedCategories.isNotEmpty() ||
                            minPrice.isNotBlank() ||
                            maxPrice.isNotBlank()
                        )
                    }
                }

                // Unified Search Panel
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(28.dp)),
                    color = Color.White,
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Column {
                        // Search Bar Area
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.onSearchQueryChange(it) },
                                placeholder = { 
                                    Text(
                                        "Search food or stall",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    ) 
                                },
                                modifier = Modifier
                                    .weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { viewModel.performManualSearch() }),
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear search",
                                                tint = Maroon
                                            )
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(28.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                )
                            )
                            
                            IconButton(
                                onClick = { viewModel.toggleFilterDialog() },
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filter",
                                    tint = Maroon
                                )
                            }

                            IconButton(
                                onClick = { viewModel.performManualSearch() },
                                modifier = Modifier
                                    .padding(4.dp)
                                    .background(
                                        Maroon,
                                        RoundedCornerShape(24.dp)
                                    )
                                    .size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (hasActiveFilterOrSearch) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 1.dp,
                                color = Color.LightGray
                            )

                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .heightIn(max = 500.dp)
                            ) {
                                if (selectedCategories.isNotEmpty() || minPrice.isNotBlank() || maxPrice.isNotBlank()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "Filters: ${selectedCategories.size} categories" + 
                                                   (if (minPrice.isNotBlank() || maxPrice.isNotBlank()) ", price range" else ""),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                if (searchResults.isEmpty()) {
                                    if (!isSearching) {
                                        Text(
                                            text = "No results found",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    } else {
                                        // Optional: Show a small loading indicator or just empty space
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.dp,
                                                color = Maroon
                                            )
                                        }
                                    }
                                } else {
                                    val listState = rememberLazyListState()
                                    LazyColumn(
                                        state = listState,
                                        modifier = Modifier.scrollbar(listState, autoHide = true)
                                    ) {
                                        items(
                                            items = searchResults,
                                            key = { it.id },
                                            contentType = { "food_card" }
                                        ) { record ->
                                            FoodItemCard(record, onClick = { viewModel.selectResult(record) })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // LAYER 3: 3-Stage Bottom Sheet
            if (viewModel.showBottomSheet) {
                val nestedScrollConnection = remember(anchoredDraggableState, fullOffset) {
                    object : NestedScrollConnection {
                        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                            val delta = available.y
                            // Swiping UP (delta < 0) - Only consume if the sheet can move up
                            return if (delta < 0 && source == NestedScrollSource.UserInput && 
                                anchoredDraggableState.requireOffset() > fullOffset) {
                                val consumed = anchoredDraggableState.dispatchRawDelta(delta)
                                Offset(x = 0f, y = consumed)
                            } else {
                                Offset.Zero
                            }
                        }

                        override fun onPostScroll(
                            consumed: Offset,
                            available: Offset,
                            source: NestedScrollSource
                        ): Offset {
                            val delta = available.y
                            // Swiping DOWN (delta > 0)
                            return if (delta > 0 && source == NestedScrollSource.UserInput) {
                                val dragConsumed = anchoredDraggableState.dispatchRawDelta(delta)
                                Offset(x = 0f, y = dragConsumed)
                            } else {
                                Offset.Zero
                            }
                        }

                        override suspend fun onPreFling(available: Velocity): Velocity {
                            val toFling = available.y
                            // Swiping UP (toFling < 0) - Only consume if the sheet can move up
                            return if (toFling < 0 && anchoredDraggableState.requireOffset() > fullOffset) {
                                val scrollScope = object : androidx.compose.foundation.gestures.ScrollScope {
                                    override fun scrollBy(pixels: Float): Float {
                                        return anchoredDraggableState.dispatchRawDelta(pixels)
                                    }
                                }
                                with(flingBehavior) {
                                    scrollScope.performFling(toFling)
                                }
                                available
                            } else {
                                Velocity.Zero
                            }
                        }

                        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                            val toFling = available.y
                            // Swiping DOWN (toFling > 0) - Always handle what's left to move sheet down
                            if (toFling > 0) {
                                val scrollScope = object : androidx.compose.foundation.gestures.ScrollScope {
                                    override fun scrollBy(pixels: Float): Float {
                                        return anchoredDraggableState.dispatchRawDelta(pixels)
                                    }
                                }
                                with(flingBehavior) {
                                    scrollScope.performFling(toFling)
                                }
                                return available
                            }
                            return Velocity.Zero
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .graphicsLayer {
                            translationY = anchoredDraggableState.requireOffset()
                        }
                        .fillMaxSize()
                        .anchoredDraggable(
                            state = anchoredDraggableState,
                            orientation = Orientation.Vertical,
                            flingBehavior = flingBehavior
                        )
                        .nestedScroll(nestedScrollConnection),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    color = Color.White,
                    shadowElevation = 16.dp
                ) {
                    Column {
                        // Custom Drag Handle
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 32.dp, height = 4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.LightGray)
                            )
                        }

                        StallBottomSheetContent(
                            stallName = viewModel.selectedStallName ?: "",
                            stallId = viewModel.selectedStallId ?: "",
                            foods = viewModel.stallFoods,
                            onDismiss = { viewModel.clearSelection() },
                            modifier = Modifier.weight(1f),
                            stallImages = viewModel.selectedStallImages,
                            progressProvider = sheetProgressProvider,
                            isKept = viewModel.isCurrentStallKept,
                            onToggleKeep = { viewModel.toggleKeepStall(viewModel.selectedStallId ?: "") }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Results Visible")
@Composable
fun MainScreenResultsPreview() {
    val context = LocalContext.current
    val repository = remember { FoodRepository(context) }
    val viewModel: MainViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(repository) as T
            }
        }
    )
    
    // Force results to show for preview
    LaunchedEffect(Unit) {
        viewModel.onSearchQueryChange("Burger")
        viewModel.performManualSearch()
    }

    PuplagoonappTheme {
        MainScreen(viewModelOverride = viewModel)
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Dark Mode"
)
@Composable
fun MainScreenPreview() {
    PuplagoonappTheme {
        MainScreen()
    }
}
