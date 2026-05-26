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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val repository = remember { FoodRepository(context) }
    val viewModel: MainViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(repository) as T
            }
        }
    )

    if (viewModel.showFilterDialog) {
        FilterDialog(
            categories = viewModel.getAllCategories(),
            selectedCategories = viewModel.selectedCategories,
            minPrice = viewModel.minPrice,
            maxPrice = viewModel.maxPrice,
            onDismiss = { viewModel.toggleFilterDialog() },
            onApply = { categories, min, max ->
                viewModel.onApplyFilters(categories, min, max)
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        " PUPili - Lagoon Food Helper",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.pup_logo),
                        contentDescription = "PUP Logo",
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Maroon
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // LAYER 1: Background Map Image
            val mapPainter = painterResource(id = R.drawable.university_map)
            val mapSize = mapPainter.intrinsicSize
            
            ZoomableBox(
                modifier = Modifier.fillMaxSize(),
                contentAspectRatio = if (mapSize.width > 0) mapSize.width / mapSize.height else 1f,
                initialCenterPixel = Offset(1818f, 1281f),
                targetCenterPixel = viewModel.selectedStallLocation,
                contentFullSize = IntSize(mapSize.width.toInt(), mapSize.height.toInt())
            ) {
                Image(
                    painter = mapPainter,
                    contentDescription = "University Map",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds, // Fill the ZoomableBox's base size
                    alpha = 1.0f
                )
            }

            // LAYER 2: UI Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Search Bar Area
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(
                            Color.White,
                            RoundedCornerShape(28.dp)
                        )
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = viewModel.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        placeholder = { Text("Search by food name") },
                        modifier = Modifier
                            .weight(1f),
                        singleLine = true,
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
                            tint = if (viewModel.selectedCategories.isNotEmpty() || viewModel.minPrice.isNotBlank() || viewModel.maxPrice.isNotBlank()) {
                                Maroon
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }

                    IconButton(
                        onClick = { viewModel.updateResultsVisibility(true) },
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

                val hasActiveFilterOrSearch by remember {
                    derivedStateOf {
                        viewModel.showResults && (
                                viewModel.searchQuery.isNotBlank() ||
                                        viewModel.selectedCategories.isNotEmpty() ||
                                        viewModel.minPrice.isNotBlank() ||
                                        viewModel.maxPrice.isNotBlank()
                                )
                    }
                }

                if (hasActiveFilterOrSearch) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Results container
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .clip(RoundedCornerShape(16.dp)),
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .heightIn(max = 500.dp)
                        ) {
                            if (viewModel.selectedCategories.isNotEmpty() || viewModel.minPrice.isNotBlank() || viewModel.maxPrice.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Filters: ${viewModel.selectedCategories.size} categories" + 
                                               (if (viewModel.minPrice.isNotBlank() || viewModel.maxPrice.isNotBlank()) ", price range" else ""),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            if (viewModel.searchResults.isEmpty()) {
                                Text(
                                    text = "No results found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(8.dp)
                                )
                            } else {
                                val listState = rememberLazyListState()
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.scrollbar(listState, autoHide = true)
                                ) {
                                    items(
                                        items = viewModel.searchResults,
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
