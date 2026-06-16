package com.example.pup_lagoon_app

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.*
import com.example.pup_lagoon_app.ui.components.LoadingOverlay
import com.example.pup_lagoon_app.ui.components.StallBottomSheetContent
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.ui.platform.LocalDensity
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
import com.example.pup_lagoon_app.ui.components.FilterDialog
import com.example.pup_lagoon_app.ui.components.FoodItemCard
import com.example.pup_lagoon_app.ui.components.ZoomableBox
import com.example.pup_lagoon_app.ui.theme.Maroon
import com.example.pup_lagoon_app.ui.theme.PuplagoonappTheme
import com.example.pup_lagoon_app.ui.utils.scrollbar
import com.example.pup_lagoon_app.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import com.example.pup_lagoon_app.ui.components.OnboardingScreen
import com.example.pup_lagoon_app.ui.components.FeatureTutorial

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PuplagoonappTheme {
                val context = LocalContext.current
                val repository = remember { FoodRepository(context) }
                val viewModel: MainViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return MainViewModel(repository) as T
                        }
                    }
                )

                if (viewModel.showOnboarding) {
                    OnboardingScreen(onComplete = { viewModel.completeOnboarding() })
                } else {
                    Box {
                        MainScreen(viewModel)
                        
                        if (viewModel.showTutorial) {
                            FeatureTutorial(
                                step = viewModel.tutorialStep,
                                onNext = { viewModel.nextTutorialStep() },
                                onSkip = { viewModel.completeTutorial() }
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class SheetStage { Minimized, Halfway, Full }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    if (viewModel.showFilterDialog) {
        val selectedCategories by viewModel.selectedCategories.collectAsState()
        val minPrice by viewModel.minPrice.collectAsState()
        val maxPrice by viewModel.maxPrice.collectAsState()

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
            
            val anchors = remember(screenHeight, density) {
                val minimizedOffset = screenHeight - with(density) { 100.dp.toPx() }
                val halfwayOffset = screenHeight * 0.53f
                val fullOffset = with(density) { 40.dp.toPx() }
                DraggableAnchors {
                    SheetStage.Minimized at minimizedOffset
                    SheetStage.Halfway at halfwayOffset
                    SheetStage.Full at fullOffset
                }
            }

            val snapAnimationSpec = remember {
                spring<Float>(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = 3000f
                )
            }

            val anchoredDraggableState = remember(anchors) {
                AnchoredDraggableState(
                    initialValue = if (viewModel.bottomSheetStage == SheetStage.Minimized && viewModel.selectedStallId != null) 
                        SheetStage.Halfway else viewModel.bottomSheetStage,
                    anchors = anchors
                )
            }

            // Sync state back to ViewModel
            LaunchedEffect(anchoredDraggableState.currentValue) {
                viewModel.bottomSheetStage = anchoredDraggableState.currentValue
                if (anchoredDraggableState.currentValue != SheetStage.Minimized) {
                    viewModel.lastActiveStage = anchoredDraggableState.currentValue
                }
            }

            val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
                state = anchoredDraggableState,
                positionalThreshold = { distance: Float -> distance * 0.4f },
                animationSpec = snapAnimationSpec
            )

            // Animate to the last active stage when a new stall is selected
            LaunchedEffect(viewModel.selectedStallId) {
                if (viewModel.selectedStallId != null) {
                    if (!viewModel.hasInteractedWithSheet) {
                        // First time opening: go to Halfway
                        anchoredDraggableState.animateTo(SheetStage.Halfway)
                        viewModel.hasInteractedWithSheet = true
                    } else {
                        // Subsequent clicks: Only expand if it's NOT currently minimized
                        if (anchoredDraggableState.currentValue != SheetStage.Minimized) {
                            anchoredDraggableState.animateTo(viewModel.lastActiveStage)
                        }
                    }
                }
            }

            // LAYER 1: Background Map Image
            MapLayer(viewModel, anchoredDraggableState, scope)

            // LAYER 2: UI Search Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, top = 40.dp, bottom = 0.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SearchLayer(viewModel)
                GuidanceLayer(viewModel)
            }

            // LAYER 3: 3-Stage Bottom Sheet
            if (viewModel.showBottomSheet) {
                BottomSheetLayer(viewModel, anchoredDraggableState, flingBehavior)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MapLayer(
    viewModel: MainViewModel,
    anchoredDraggableState: AnchoredDraggableState<SheetStage>,
    scope: CoroutineScope
) {
    val mapPainter = painterResource(id = R.drawable.university_map)
    val mapSize = mapPainter.intrinsicSize
    
    Box(modifier = Modifier.fillMaxSize()) {
        ZoomableBox(
            modifier = Modifier.fillMaxSize(),
            contentAspectRatio = if (mapSize.width > 0) mapSize.width / mapSize.height else 1f,
            initialScale = 1.7f,
            initialCenterPixel = Offset(1787f, 1272f),
            targetCenterPixel = viewModel.selectedStallLocation,
            selectedStallIds = viewModel.selectedStallIds,
            selectedStallLocations = viewModel.selectedStallLocations,
            contentFullSize = IntSize(mapSize.width.toInt(), mapSize.height.toInt()),
            keptPins = viewModel.keptStallLocations,
            mapLabels = viewModel.mapLabels,
            navigationPath = viewModel.navigationPath,
            selectedGateId = viewModel.selectedGateId,
            allStallLocations = viewModel.allStallLocations,
            isLoading = viewModel.isMapLoading,
            onPinClick = { stallId ->
                viewModel.selectStallById(stallId)
            },
            onLandmarkClick = { landmarkId ->
                if (viewModel.selectedStallId != null) {
                    viewModel.updateRoute(landmarkId)
                }
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
                contentScale = ContentScale.Fit,
                alpha = 1.0f
            )

            // Trigger onMapReady when the image is rendered
            LaunchedEffect(Unit) {
                viewModel.onMapReady()
            }
        }

        // Place LoadingOverlay OUTSIDE ZoomableBox so it's not affected by pans/zooms
        LoadingOverlay(isLoading = viewModel.isMapLoading)
    }
}

@Composable
private fun SearchLayer(viewModel: MainViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val selectedCategories by viewModel.selectedCategories.collectAsState()
    val minPrice by viewModel.minPrice.collectAsState()
    val maxPrice by viewModel.maxPrice.collectAsState()

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

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(28.dp)),
        color = Color.White,
        shape = RoundedCornerShape(28.dp)
    ) {
        Column {
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
                    modifier = Modifier.weight(1f),
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
                        .background(Maroon, RoundedCornerShape(24.dp))
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

@Composable
private fun GuidanceLayer(viewModel: MainViewModel) {
    if (viewModel.guidanceText != null && !viewModel.showResults) {
        Spacer(modifier = Modifier.height(8.dp))
        
        if (viewModel.isGuidanceMinimized) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                IconButton(
                    onClick = { viewModel.isGuidanceMinimized = false },
                    modifier = Modifier
                        .padding(top = 4.dp, bottom = 4.dp, start = 4.dp, end = 0.dp)
                        .shadow(4.dp, CircleShape)
                        .background(Maroon, CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Directions,
                        contentDescription = "Show directions",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                color = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Directions,
                            contentDescription = null,
                            tint = Maroon,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Directions from:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        
                        IconButton(
                            onClick = { viewModel.isGuidanceMinimized = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Minimize",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = viewModel.guidanceText ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BottomSheetLayer(
    viewModel: MainViewModel,
    anchoredDraggableState: AnchoredDraggableState<SheetStage>,
    flingBehavior: androidx.compose.foundation.gestures.FlingBehavior
) {
    val density = LocalDensity.current
    val fullOffset = with(density) { 40.dp.toPx() }

    val nestedScrollConnection = remember(anchoredDraggableState, fullOffset, flingBehavior) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                return if (delta < 0 && source == NestedScrollSource.UserInput && 
                    anchoredDraggableState.requireOffset() > fullOffset) {
                    val consumed = anchoredDraggableState.dispatchRawDelta(delta)
                    Offset(x = 0f, y = consumed)
                } else Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                return if (delta > 0 && source == NestedScrollSource.UserInput) {
                    val dragConsumed = anchoredDraggableState.dispatchRawDelta(delta)
                    Offset(x = 0f, y = dragConsumed)
                } else Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val toFling = available.y
                return if (toFling < 0 && anchoredDraggableState.requireOffset() > fullOffset) {
                    val scrollScope = object : androidx.compose.foundation.gestures.ScrollScope {
                        override fun scrollBy(pixels: Float): Float = anchoredDraggableState.dispatchRawDelta(pixels)
                    }
                    with(flingBehavior) { scrollScope.performFling(toFling) }
                    available
                } else Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val toFling = available.y
                if (toFling > 0) {
                    val scrollScope = object : androidx.compose.foundation.gestures.ScrollScope {
                        override fun scrollBy(pixels: Float): Float = anchoredDraggableState.dispatchRawDelta(pixels)
                    }
                    with(flingBehavior) { scrollScope.performFling(toFling) }
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    val sheetProgressProvider = remember(anchoredDraggableState) {
        {
            val anchors = anchoredDraggableState.anchors
            val min = anchors.positionOf(SheetStage.Minimized)
            val full = anchors.positionOf(SheetStage.Full)
            val offset = try { anchoredDraggableState.requireOffset() } catch (_: Exception) { min }
            val totalRange = min - full
            if (totalRange > 0) ((min - offset) / totalRange).coerceIn(0f, 1f) else 0f
        }
    }

    Surface(
        modifier = Modifier
            .graphicsLayer { translationY = anchoredDraggableState.requireOffset() }
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
                stallId = viewModel.displayStallId ?: "",
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

@Preview(showBackground = true, name = "Results Visible")
@Composable
fun MainScreenResultsPreview() {
    val context = LocalContext.current
    val repository = remember { FoodRepository(context) }
    val viewModel: MainViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(repository) as T
            }
        }
    )
    
    LaunchedEffect(Unit) {
        viewModel.onSearchQueryChange("Burger")
        viewModel.performManualSearch()
    }

    PuplagoonappTheme {
        MainScreen(viewModel = viewModel)
    }
}

@Preview(showBackground = true, name = "Guidance Minimized")
@Composable
fun GuidanceMinimizedPreview() {
    val context = LocalContext.current
    val repository = remember { FoodRepository(context) }
    val viewModel: MainViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(repository) as T
            }
        }
    )
    
    LaunchedEffect(Unit) {
        viewModel.selectStallById("1") // Select a stall to show guidance
        viewModel.isGuidanceMinimized = true
    }

    PuplagoonappTheme {
        MainScreen(viewModel = viewModel)
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
    val context = LocalContext.current
    val repository = remember { FoodRepository(context) }
    val viewModel: MainViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(repository) as T
            }
        }
    )
    PuplagoonappTheme {
        MainScreen(viewModel = viewModel)
    }
}
