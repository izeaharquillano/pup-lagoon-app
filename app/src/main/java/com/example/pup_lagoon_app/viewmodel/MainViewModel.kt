package com.example.pup_lagoon_app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import com.example.pup_lagoon_app.data.FoodRecord
import com.example.pup_lagoon_app.data.MergedRecords
import com.example.pup_lagoon_app.data.FoodRepository


class TrigramSearchIndex(records: List<FoodRecord>) {

    private val index = HashMap<String, MutableList<String>>()
    private val recordMap = HashMap<String, FoodRecord>()

    init {
        records.forEach { record ->
            recordMap[record.foodId] = record
            trigrams(record.name.lowercase()).forEach { tri ->
                index.getOrPut(tri) { mutableListOf() }.add(record.foodId)
            }
        }
    }

    fun search(query: String, topN: Int = 20): List<FoodRecord> {
        val queryTrigrams = trigrams(query.lowercase())
        val scores = HashMap<String, Int>()

        queryTrigrams.forEach { tri ->
            index[tri]?.forEach { id ->
                scores[id] = (scores[id] ?: 0) + 1
            }
        }

        return scores.entries
            .sortedByDescending { it.value }
            .take(topN)
            .mapNotNull { recordMap[it.key] }
    }

    private fun trigrams(s: String): List<String> {
        if (s.length < 3) return listOf(s.padEnd(3))
        return (0..s.length - 3).map { s.substring(it, it + 3) }
    }
}

class MainViewModel(private val repository: FoodRepository) : ViewModel() {

    // Built once when the ViewModel is created
    private val searchIndex = TrigramSearchIndex(repository.getAllRecords())

    var searchQuery by mutableStateOf("")
        private set

    var selectedCategories by mutableStateOf(setOf<String>())
        private set

    var minPrice by mutableStateOf("")
        private set

    var maxPrice by mutableStateOf("")
        private set

    var showFilterDialog by mutableStateOf(false)

    var showResults by mutableStateOf(true)
        private set

    var selectedStallLocation by mutableStateOf<Offset?>(null)
        private set

    val searchResults: List<MergedRecords> by derivedStateOf {
        val min = minPrice.toDoubleOrNull() ?: 0.0
        val max = maxPrice.toDoubleOrNull() ?: Double.MAX_VALUE

        // Use trigram search if there's a query, otherwise fetch everything
        val initialSet = if (searchQuery.isNotBlank()) {
            searchIndex.search(searchQuery)
        } else {
            repository.getAllRecords()
        }

        // Category and price filters still applied on top
        val rawResults = initialSet.filter { record ->
            val matchesCategory = if (selectedCategories.isNotEmpty()) {
                selectedCategories.all { it in record.categories }
            } else true

            val matchesPrice = record.numericPrice in min..max

            matchesCategory && matchesPrice
        }.distinctBy { it.foodId }

        groupFoodRecords(rawResults)
    }

    fun onSearchQueryChange(newQuery: String) {
        searchQuery = newQuery
        showResults = true
    }

    fun onApplyFilters(categories: Set<String>, min: String, max: String) {
        selectedCategories = categories
        minPrice = min
        maxPrice = max
        showFilterDialog = false
        showResults = true
    }

    fun toggleFilterDialog() {
        showFilterDialog = !showFilterDialog
    }

    fun selectResult(record: MergedRecords) {
        val location = repository.getStallLocation(record.stallId)
        selectedStallLocation = location?.toOffset()
        showResults = false
    }

    fun updateResultsVisibility(show: Boolean) {
        showResults = show
    }

    fun clearSelection() {
        selectedStallLocation = null
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