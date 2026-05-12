package com.example.pup_lagoon_app
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.pup_lagoon_app.data.FoodRecord
import com.example.pup_lagoon_app.data.FoodRepository
import com.example.pup_lagoon_app.ui.theme.Maroon
import com.example.pup_lagoon_app.ui.theme.PuplagoonappTheme

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
    var searchQuery by remember { mutableStateOf("") }
    
    // Filter State
    var selectedCategories by remember { mutableStateOf(setOf<String>()) }
    var minPrice by remember { mutableStateOf("") }
    var maxPrice by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }

    val searchResults = remember(searchQuery, selectedCategories, minPrice, maxPrice) {
        val min = minPrice.toDoubleOrNull() ?: 0.0
        val max = maxPrice.toDoubleOrNull() ?: Double.MAX_VALUE

        // 1. Get initial set from B-Tree based on name or price range
        val initialSet = if (searchQuery.isNotBlank()) {
            repository.searchByName(searchQuery) ?: emptyList()
        } else if (minPrice.isNotBlank() || maxPrice.isNotBlank()) {
            repository.searchByPriceRange(min, max)
        } else if (selectedCategories.isNotEmpty()) {
            // If only categories, we could pick one category from B-Tree
            repository.searchByCategory(selectedCategories.first()) ?: emptyList()
        } else {
            repository.getAllRecords()
        }

        // 2. Filter the initial set in memory for other criteria (Intersection)
        initialSet.filter { record ->
            val matchesName = if (searchQuery.isNotBlank()) {
                record.name.contains(searchQuery, ignoreCase = true)
            } else true
            
            val matchesCategory = if (selectedCategories.isNotEmpty()) {
                selectedCategories.all { it in record.categories }
            } else true
            
            val matchesPrice = record.numericPrice in min..max
            
            matchesName && matchesCategory && matchesPrice
        }.distinctBy { it.foodId } // Ensure unique results
    }

    if (showFilterDialog) {
        FilterDialog(
            categories = repository.getAllCategories(),
            selectedCategories = selectedCategories,
            minPrice = minPrice,
            maxPrice = maxPrice,
            onDismiss = { showFilterDialog = false },
            onApply = { categories, min, max ->
                selectedCategories = categories
                minPrice = min
                maxPrice = max
                showFilterDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Lagoon Food Helper",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
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
            Image(
                painter = painterResource(id = R.drawable.university_map),
                contentDescription = "University Map",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 1.0f
            )

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
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(28.dp)
                        )
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
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
                        onClick = { showFilterDialog = true },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = if (selectedCategories.isNotEmpty() || minPrice.isNotBlank() || maxPrice.isNotBlank()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }

                    IconButton(
                        onClick = { /* Search is real-time */ },
                        modifier = Modifier
                            .padding(4.dp)
                            .background(
                                if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else Maroon,
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

                val hasActiveFilterOrSearch = searchQuery.isNotBlank() || 
                                              selectedCategories.isNotEmpty() || 
                                              minPrice.isNotBlank() || 
                                              maxPrice.isNotBlank()

                if (hasActiveFilterOrSearch) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Results container
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .clip(RoundedCornerShape(16.dp)),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .heightIn(max = 500.dp)
                        ) {
                            if (selectedCategories.isNotEmpty() || minPrice.isNotBlank() || maxPrice.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
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
                                    items(searchResults) { record ->
                                        FoodItemCard(record)
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

@Composable
fun FilterDialog(
    categories: List<String>,
    selectedCategories: Set<String>,
    minPrice: String,
    maxPrice: String,
    onDismiss: () -> Unit,
    onApply: (Set<String>, String, String) -> Unit
) {
    var tempSelectedCategories by remember { mutableStateOf(selectedCategories) }
    var tempMinPrice by remember { mutableStateOf(minPrice) }
    var tempMaxPrice by remember { mutableStateOf(maxPrice) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Filter Options",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Price Range",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = tempMinPrice,
                        onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) tempMinPrice = it },
                        label = { Text("Min") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tempMaxPrice,
                        onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) tempMaxPrice = it },
                        label = { Text("Max") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .height(200.dp)
                        .fillMaxWidth()
                        .scrollbar(listState, autoHide = false)
                ) {
                    items(categories) { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .toggleable(
                                    value = category in tempSelectedCategories,
                                    onValueChange = {
                                        tempSelectedCategories = if (it) {
                                            tempSelectedCategories + category
                                        } else {
                                            tempSelectedCategories - category
                                        }
                                    },
                                    role = Role.Checkbox
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = category in tempSelectedCategories,
                                onCheckedChange = null // null because of toggleable
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = category,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        tempSelectedCategories = emptySet()
                        tempMinPrice = ""
                        tempMaxPrice = ""
                    }) {
                        Text("Clear All")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onApply(tempSelectedCategories, tempMinPrice, tempMaxPrice) }) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

@Composable
fun FoodItemCard(record: FoodRecord) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = record.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = record.price,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "Stall: ${record.stallName}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Categories: ${record.categories.joinToString(", ")}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun Modifier.scrollbar(
    state: LazyListState,
    thickness: Dp = 4.dp,
    color: Color = Color.Gray.copy(alpha = 0.5f),
    autoHide: Boolean = true
): Modifier {
    val alpha by animateFloatAsState(
        targetValue = if (!autoHide || state.isScrollInProgress) 1f else 0f,
        animationSpec = tween(durationMillis = if (state.isScrollInProgress) 0 else 500),
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

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
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