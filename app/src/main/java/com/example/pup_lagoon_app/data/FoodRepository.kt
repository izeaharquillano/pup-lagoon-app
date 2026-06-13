package com.example.pup_lagoon_app.data

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

class FoodRepository(private val context: Context) {
    private val nameTree = BTree<String, FoodRecord>(5)
    private val categoryTree = BTree<String, FoodRecord>(5)
    private val priceTree = BTree<Double, FoodRecord>(5)
    private val allCategories = mutableSetOf<String>()
    private val stallLocations = mutableMapOf<String, StallLocation>()

    init {
        loadStallLocations()
        loadFromCsv()
    }

    private fun loadStallLocations() {
        try {
            val inputStream = context.assets.open("stall_locations.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.readLine() // Skip header
            
            var line: String? = reader.readLine()
            while (line != null) {
                val tokens = line.split(",")
                if (tokens.size >= 3) {
                    val id = tokens[0].trim()
                    val x = tokens[1].trim().toFloatOrNull() ?: 0f
                    val y = tokens[2].trim().toFloatOrNull() ?: 0f
                    stallLocations[id] = StallLocation(id, x, y)
                }
                line = reader.readLine()
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadFromCsv() {
        try {
            val inputStream = context.assets.open("food_records.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            // Skip header
            reader.readLine()
            
            var line: String? = reader.readLine()
            while (line != null) {
                val tokens = parseCsvLine(line)
                if (tokens.size >= 6) {
                    val categories = tokens[5].split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    
                    allCategories.addAll(categories)
                    
                    val numericPrice = tokens[4].replace("₱", "")
                        .replace(",", "")
                        .toDoubleOrNull() ?: 0.0

                    val record = FoodRecord(
                        stallId = tokens[0],
                        stallName = tokens[1],
                        foodId = tokens[2],
                        name = tokens[3],
                        price = tokens[4],
                        numericPrice = numericPrice,
                        categories = categories
                    )
                    
                    // Index by name (lowercase for case-insensitive prefix search)
                    nameTree.insert(record.name.lowercase(), record)
                    
                    // Index by categories
                    categories.forEach { category ->
                        categoryTree.insert(category, record)
                    }
                    
                    // Index by price
                    priceTree.insert(numericPrice, record)
                }
                line = reader.readLine()
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var currentToken = StringBuilder()
        var inQuotes = false
        
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                result.add(currentToken.toString().trim())
                currentToken = StringBuilder()
            } else {
                currentToken.append(c)
            }
            i++
        }
        result.add(currentToken.toString().trim())
        return result
    }

    fun search(
        nameQuery: String = "",
        selectedCategories: Set<String> = emptySet(),
        minPrice: Double = 0.0,
        maxPrice: Double = Double.MAX_VALUE
    ): List<FoodRecord> {
        val queryLower = nameQuery.lowercase()
        val predicate: (FoodRecord) -> Boolean = { record ->
            val matchesName = if (queryLower.isNotBlank()) {
                record.name.contains(queryLower, ignoreCase = true)
            } else true
            
            val matchesCategory = if (selectedCategories.isNotEmpty()) {
                selectedCategories.all { it in record.categories }
            } else true
            
            val matchesPrice = record.numericPrice in minPrice..maxPrice
            
            matchesName && matchesCategory && matchesPrice
        }

        return when {
            // If specific categories are selected, use categoryTree for one of them
            selectedCategories.isNotEmpty() -> {
                val firstCategory = selectedCategories.first()
                categoryTree.search(firstCategory, predicate)
            }
            
            // If price range is restrictive, use priceTree
            minPrice > 0.0 || maxPrice < Double.MAX_VALUE -> {
                priceTree.searchRange(minPrice, maxPrice, predicate)
            }
            
            // Optimization: If name is provided and nothing else is restrictive,
            // we use the nameTree to jump to prefix matches first.
            queryLower.isNotBlank() -> {
                // 1. Get prefix matches instantly from the nameTree (Case-insensitive prefix)
                val prefixResults = nameTree.searchRange(queryLower, queryLower + "\uFFFF", predicate)
                
                // 2. We still need infix matches (e.g., search "burger" finds "Cheeseburger").
                // Since these could be anywhere, we scan the whole repository,
                // but we skip the ones we already found via prefix to avoid duplicates.
                val prefixIds = prefixResults.map { it.foodId }.toSet()
                
                val infixResults = priceTree.searchRange(0.0, Double.MAX_VALUE) { record ->
                    record.foodId !in prefixIds && predicate(record)
                }
                
                prefixResults + infixResults
            }
            
            else -> {
                priceTree.searchRange(0.0, Double.MAX_VALUE, predicate)
            }
        }
    }

    fun getAllCategories(): List<String> {
        return allCategories.toList().sorted()
    }

    fun getAllRecords(): List<FoodRecord> {
        return priceTree.searchRange(0.0, Double.MAX_VALUE)
    }

    fun getFoodsByStall(stallId: String): List<FoodRecord> {
        return priceTree.searchRange(0.0, Double.MAX_VALUE).filter { it.stallId == stallId }
    }

    fun getStallLocation(stallId: String): StallLocation? {
        return stallLocations[stallId]
    }

    fun getStallImages(stallId: String): List<String> {
        return try {
            val path = "stalls/$stallId"
            context.assets.list(path)
                ?.filter { it.endsWith(".jpg", ignoreCase = true) || it.endsWith(".png", ignoreCase = true) || it.endsWith(".webp", ignoreCase = true) }
                ?.map { "file:///android_asset/$path/$it" }
                ?.sorted()
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
