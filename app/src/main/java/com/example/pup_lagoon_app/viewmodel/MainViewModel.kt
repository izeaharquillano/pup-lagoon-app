package com.example.pup_lagoon_app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.lifecycle.ViewModel
import com.example.pup_lagoon_app.data.FoodRecord
import com.example.pup_lagoon_app.data.MergedRecords
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

    val searchResults: List<MergedRecords> by derivedStateOf {
        val min = minPrice.toDoubleOrNull() ?: 0.0
        val max = maxPrice.toDoubleOrNull() ?: Double.MAX_VALUE

        val initialSet = if (searchQuery.isNotBlank()) {
            repository.searchByName(searchQuery) ?: emptyList()
        } else if (minPrice.isNotBlank() || maxPrice.isNotBlank()) {
            repository.searchByPriceRange(min, max)
        } else if (selectedCategories.isNotEmpty()) {
            repository.searchByCategory(selectedCategories.first()) ?: emptyList()
        } else {
            repository.getAllRecords()
        }

        val rawResults = initialSet.filter { record ->
            val matchesName = if (searchQuery.isNotBlank()) {
                record.name.contains(searchQuery, ignoreCase = true)
            } else true

            val matchesCategory = if (selectedCategories.isNotEmpty()) {
                selectedCategories.all { it in record.categories }
            } else true

            val matchesPrice = record.numericPrice in min..max

            matchesName && matchesCategory && matchesPrice
        }.distinctBy { it.foodId }

        // Group the raw results before sending to UI
        groupFoodRecords(rawResults)
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

    // --- Merging Logic Helpers ---

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
                            baseName = baseName,
                            stallName = stall,
                            categories = matchingRecords.first().categories,
                            sizePrices = sizePriceList,
                            priceRange = priceDisplay
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