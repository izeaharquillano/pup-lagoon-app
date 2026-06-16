package com.example.pup_lagoon_app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pup_lagoon_app.data.FoodRecord
import com.example.pup_lagoon_app.data.MergedRecords
import com.example.pup_lagoon_app.data.FoodRepository
import com.example.pup_lagoon_app.data.MapLabel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn


private data class SearchParameters(
    val query: String,
    val categories: Set<String>,
    val minStr: String,
    val maxStr: String,
    val manualActive: Boolean
)

@OptIn(FlowPreview::class)
class MainViewModel(private val repository: FoodRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategories = MutableStateFlow(setOf<String>())
    val selectedCategories: StateFlow<Set<String>> = _selectedCategories.asStateFlow()

    private val _minPrice = MutableStateFlow("")
    val minPrice: StateFlow<String> = _minPrice.asStateFlow()

    private val _maxPrice = MutableStateFlow("")
    val maxPrice: StateFlow<String> = _maxPrice.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    var showFilterDialog by mutableStateOf(false)

    var showResults by mutableStateOf(true)
        private set

    private val _manualSearchActive = MutableStateFlow(false)

    var selectedStallIds by mutableStateOf<Set<String>>(emptySet())
        private set

    var selectedStallLocation by mutableStateOf<Offset?>(null)
        private set

    var selectedStallId by mutableStateOf<String?>(null)
        private set

    var displayStallId by mutableStateOf<String?>(null)
        private set

    var selectedStallName by mutableStateOf<String?>(null)
        private set

    var selectedStallImages by mutableStateOf<List<String>>(emptyList())
        private set

    var showBottomSheet by mutableStateOf(false)

    var navigationPath by mutableStateOf<List<Offset>>(emptyList())
        private set

    var guidanceText by mutableStateOf<String?>(null)
        private set

    var availableGates by mutableStateOf<List<MapLabel>>(emptyList())
        private set

    var selectedGateId by mutableStateOf<String?>(null)
        private set

    var isGuidanceMinimized by mutableStateOf(false)

    var keptStallIds by mutableStateOf(setOf<String>())
        private set

    val isCurrentStallKept: Boolean by derivedStateOf {
        selectedStallId?.let { it in keptStallIds } ?: false
    }

    val selectedStallLocations: Map<String, Offset> by derivedStateOf {
        selectedStallIds.associateWith { stallId ->
            repository.getStallLocation(stallId)?.toOffset() ?: Offset.Zero
        }.filter { it.value != Offset.Zero }
    }

    val keptStallLocations: Map<String, Offset> by derivedStateOf {
        keptStallIds.associateWith { stallId ->
            repository.getStallLocation(stallId)?.toOffset() ?: Offset.Zero
        }.filter { it.value != Offset.Zero }
    }

    val stallFoods: List<MergedRecords> by derivedStateOf {
        val stallId = selectedStallId ?: return@derivedStateOf emptyList<MergedRecords>()
        val foods = repository.getFoodsByStall(stallId)
        groupFoodRecords(foods)
    }

    val mapLabels: List<MapLabel> = repository.getMapLabels()

    val searchResults: StateFlow<List<MergedRecords>> = combine(
        _searchQuery,
        _selectedCategories,
        _minPrice,
        _maxPrice,
        _manualSearchActive
    ) { query, categories, minStr, maxStr, manualActive ->
        SearchParameters(query, categories, minStr, maxStr, manualActive)
    }.map { params ->
        val isQueryLongEnough = params.query.length >= 2
        val isFilterActive = params.categories.isNotEmpty() || params.minStr.isNotBlank() || params.maxStr.isNotBlank()
        val shouldSearch = isQueryLongEnough || params.manualActive || isFilterActive
        
        if (shouldSearch) {
            _isSearching.value = true
        }
        params
    }.debounce { params ->
        // Immediate clear when query is empty, otherwise debounce
        if (params.query.isEmpty() || params.manualActive) 0L else 200L
    }.map { params ->
        val min = params.minStr.toDoubleOrNull() ?: 0.0
        val max = params.maxStr.toDoubleOrNull() ?: Double.MAX_VALUE

        val isQueryLongEnough = params.query.length >= 2
        val isFilterActive = params.categories.isNotEmpty() || params.minStr.isNotBlank() || params.maxStr.isNotBlank()
        val shouldSearch = isQueryLongEnough || params.manualActive || isFilterActive

        val results = if (!shouldSearch) {
            emptyList<MergedRecords>()
        } else {
            val rawResults = repository.search(
                nameQuery = params.query,
                selectedCategories = params.categories,
                minPrice = min,
                maxPrice = max
            ).distinctBy { it.foodId }

            groupFoodRecords(rawResults)
        }
        _isSearching.value = false
        results
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
        _manualSearchActive.value = false // Reset manual trigger on typing
        
        if (newQuery.isNotEmpty()) {
            showResults = true
        } else {
            // If clearing the search query, and no filters are active, 
            // we should hide the results panel immediately to avoid flicker.
            val hasActiveFilters = _selectedCategories.value.isNotEmpty() || 
                                   _minPrice.value.isNotBlank() || 
                                   _maxPrice.value.isNotBlank()
            if (!hasActiveFilters) {
                showResults = false
            }
        }
    }

    fun performManualSearch() {
        _manualSearchActive.value = true
        showResults = true
    }

    fun onApplyFilters(categories: Set<String>, min: String, max: String) {
        _selectedCategories.value = categories
        _minPrice.value = min
        _maxPrice.value = max
        showFilterDialog = false
        showResults = true
    }

    fun toggleFilterDialog() {
        showFilterDialog = !showFilterDialog
    }

    fun toggleKeepStall(stallId: String) {
        val isWaterStation = stallId == "14" || stallId == "16"
        val idsToToggle = if (isWaterStation) setOf("14", "16") else setOf(stallId)
        
        val anyInKept = idsToToggle.any { it in keptStallIds }
        
        keptStallIds = if (anyInKept) {
            keptStallIds - idsToToggle
        } else {
            keptStallIds + idsToToggle
        }
    }

    fun selectStallById(stallId: String) {
        val isWaterStation = stallId == "14" || stallId == "16"
        val location = repository.getStallLocation(stallId)
        
        if (isWaterStation) {
            selectedStallIds = setOf("14", "16")
            selectedStallName = "Water Refilling Station"
            displayStallId = "14, #16"
            selectedStallLocation = location?.toOffset()
        } else {
            selectedStallIds = setOf(stallId)
            selectedStallName = repository.getAllRecords().find { it.stallId == stallId }?.stallName ?: "Unknown Stall"
            displayStallId = stallId
            selectedStallLocation = location?.toOffset()
        }

        // Calculate Route Guidance
        availableGates = mapLabels.filter { it.text.contains("Gate", ignoreCase = true) }
        location?.let { loc ->
            // Don't auto-calculate gate, just prompt user
            guidanceText = "Tap a Gate (Gate 1, 2, or 3) on the map for directions."
            navigationPath = emptyList()
            selectedGateId = null
        }
        
        selectedStallId = stallId
        selectedStallImages = repository.getStallImages(stallId)
        showResults = false
        showBottomSheet = true
    }

    fun selectResult(record: MergedRecords) {
        val queryLower = _searchQuery.value.lowercase().trim()
        val baseNameLower = record.baseName.lowercase().trim()
        
        if (queryLower == "water refilling station" || baseNameLower == "water refilling station") {
            // Special case: Select both 14 and 16
            val ids = setOf("14", "16")
            selectedStallIds = ids
            
            // Center on one of them (e.g., 14)
            val location = repository.getStallLocation("14")
            selectedStallLocation = location?.toOffset()
            selectedStallId = "14"
            selectedStallName = "Water Refilling Station"
            displayStallId = "14, #16"
            selectedStallImages = repository.getStallImages("14")
            
            // Navigation for Water Station
            availableGates = mapLabels.filter { it.text.contains("Gate", ignoreCase = true) }
            location?.let { loc ->
                guidanceText = "Tap a Gate (Gate 1, 2, or 3) on the map for directions."
                navigationPath = emptyList()
                selectedGateId = null
            }

            showResults = false
            showBottomSheet = true
        } else {
            selectStallById(record.stallId)
        }
    }

    fun updateRoute(gateId: String) {
        val gate = mapLabels.find { it.id == gateId } ?: return
        val stallId = selectedStallId ?: return
        val location = repository.getStallLocation(stallId) ?: return
        
        selectedGateId = gateId
        navigationPath = repository.calculatePath(gate, location)
        guidanceText = repository.getDirectionText(gate, location)
    }

    fun clearSelection() {
        selectedStallLocation = null
        selectedStallId = null
        selectedStallIds = emptySet()
        displayStallId = null
        selectedStallName = null
        selectedStallImages = emptyList()
        showBottomSheet = false
        navigationPath = emptyList()
        guidanceText = null
        availableGates = emptyList()
        selectedGateId = null
        isGuidanceMinimized = false
    }

    fun getAllCategories(): List<String> {
        return repository.getAllCategories()
    }



    private fun groupFoodRecords(records: List<FoodRecord>): List<MergedRecords> {
        return records.groupBy { it.stallName }
            .flatMap { (stall, stallRecords) ->
                stallRecords.groupBy { extractBaseName(it.name) }
                    .map { (baseName, matchingRecords) ->

                        val sizePriceList = matchingRecords
                            .mapNotNull { record ->
                                val size = extractSize(record.name)
                                if (size.isNotEmpty()) {
                                    size to "₱${String.format("%.0f", record.numericPrice)}"
                                } else null
                            }
                            .distinctBy { it.first }
                            .sortedWith(Comparator { p1, p2 ->
                                val order = listOf("S", "M", "L", "XL")
                                val index1 = order.indexOf(p1.first.uppercase())
                                val index2 = order.indexOf(p2.first.uppercase())
                                (if (index1 == -1) 99 else index1).compareTo(if (index2 == -1) 99 else index2)
                            })
                            .map { "${it.first} - ${it.second}" }

                        val prices = matchingRecords.map { it.numericPrice }.sorted()

                        val priceDisplay = if (prices.size > 1) {
                            "₱${String.format("%.0f", prices.first())} - ₱${String.format("%.0f", prices.last())}"
                        } else {
                            "₱${String.format("%.0f", prices.first())}"
                        }

                        MergedRecords(
                            id = "${stall}_${baseName}",
                            stallId = matchingRecords.first().stallId,
                            baseName = baseName,
                            stallName = stall,
                            categories = matchingRecords.first().categories,
                            sizePrices = sizePriceList,
                            priceRange = priceDisplay,
                            displayCategories = matchingRecords.first().categories.joinToString(", "),
                            displaySizes = sizePriceList.joinToString(", ")
                        )
                    }
            }
    }

    private fun extractBaseName(fullName: String): String {
        val pattern = Regex("\\s+(S|M|L|XL|[0-9]+pcs)$", RegexOption.IGNORE_CASE)
        return fullName.replace(pattern, "").trim()
    }

    private fun extractSize(fullName: String): String {
        val pattern = Regex("\\s+(S|M|L|XL|[0-9]+pcs)$", RegexOption.IGNORE_CASE)
        val match = pattern.find(fullName)
        return match?.value?.trim()?.uppercase() ?: ""
    }
}