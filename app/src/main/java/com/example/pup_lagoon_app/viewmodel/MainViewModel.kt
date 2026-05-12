package com.example.pup_lagoon_app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.pup_lagoon_app.data.FoodRecord
import com.example.pup_lagoon_app.data.FoodRepository

class MainViewModel(private val repository: FoodRepository) : ViewModel() {
    var searchQuery by mutableStateOf("")
        private set
    
    var selectedCategories by mutableStateOf(setOf<String>())
        private set
        
    var minPrice by mutableStateOf("")
        private set
        
    var maxPrice by mutableStateOf("")
        private set
        
    var showFilterDialog by mutableStateOf(false)

    val searchResults: List<FoodRecord>
        get() {
            val min = minPrice.toDoubleOrNull() ?: 0.0
            val max = maxPrice.toDoubleOrNull() ?: Double.MAX_VALUE

            // 1. Get initial set from B-Tree based on name or price range
            val initialSet = if (searchQuery.isNotBlank()) {
                repository.searchByName(searchQuery) ?: emptyList()
            } else if (minPrice.isNotBlank() || maxPrice.isNotBlank()) {
                repository.searchByPriceRange(min, max)
            } else if (selectedCategories.isNotEmpty()) {
                repository.searchByCategory(selectedCategories.first()) ?: emptyList()
            } else {
                repository.getAllRecords()
            }

            // 2. Filter the initial set in memory for other criteria (Intersection)
            return initialSet.filter { record ->
                val matchesName = if (searchQuery.isNotBlank()) {
                    record.name.contains(searchQuery, ignoreCase = true)
                } else true
                
                val matchesCategory = if (selectedCategories.isNotEmpty()) {
                    selectedCategories.all { it in record.categories }
                } else true
                
                val matchesPrice = record.numericPrice in min..max
                
                matchesName && matchesCategory && matchesPrice
            }.distinctBy { it.foodId }
        }

    fun onSearchQueryChange(newQuery: String) {
        searchQuery = newQuery
    }

    fun onApplyFilters(categories: Set<String>, min: String, max: String) {
        selectedCategories = categories
        minPrice = min
        maxPrice = max
        showFilterDialog = false
    }
    
    fun toggleFilterDialog() {
        showFilterDialog = !showFilterDialog
    }
    
    fun getAllCategories(): List<String> {
        return repository.getAllCategories()
    }
}
