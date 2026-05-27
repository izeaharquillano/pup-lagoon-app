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
        val predicate: (FoodRecord) -> Boolean = { record ->
            val matchesName = if (nameQuery.isNotBlank()) {
                record.name.contains(nameQuery, ignoreCase = true)
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
                // Pick one category to search in the tree, then filter others with predicate
                val firstCategory = selectedCategories.first()
                categoryTree.search(firstCategory, predicate)
            }
            
            // If price range is somewhat restrictive, use priceTree
            // (In this case, we always use it if price is specified to show B-tree usage)
            minPrice > 0.0 || maxPrice < Double.MAX_VALUE -> {
                priceTree.searchRange(minPrice, maxPrice, predicate)
            }
            
            // If name is provided and nothing else is very restrictive
            nameQuery.isNotBlank() -> {
                // Note: B-tree search by name prefix might be faster, but let's stick to priceTree/categoryTree 
                // or just scan if needed. Actually, nameTree is good for prefix.
                // But for general containment, a scan or priceTree is fine.
                priceTree.searchRange(0.0, Double.MAX_VALUE, predicate)
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

    fun getStallLocation(stallId: String): StallLocation? {
        return stallLocations[stallId]
    }
}
