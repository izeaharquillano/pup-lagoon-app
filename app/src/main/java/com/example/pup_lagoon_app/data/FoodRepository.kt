package com.example.pup_lagoon_app.data

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

class FoodRepository(private val context: Context) {
    private val nameTree = BTree<String, FoodRecord>(5)
    private val categoryTree = BTree<String, FoodRecord>(5)
    private val priceTree = BTree<Double, FoodRecord>(5)
    private val allCategories = mutableSetOf<String>()

    init {
        loadFromCsv()
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
                    
                    // Index by name
                    nameTree.insert(record.name, record)
                    
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

    fun searchByName(name: String): List<FoodRecord>? {
        return nameTree.search(name)
    }

    fun searchByCategory(category: String): List<FoodRecord>? {
        return categoryTree.search(category)
    }

    fun searchByPriceRange(min: Double, max: Double): List<FoodRecord> {
        return priceTree.searchRange(min, max)
    }

    fun getAllCategories(): List<String> {
        return allCategories.toList().sorted()
    }

    fun getAllRecords(): List<FoodRecord> {
        // Return all records by searching the entire price range or name range
        // Since we don't have a direct 'getAll' in BTree, we can use searchRange with extreme values
        return priceTree.searchRange(0.0, Double.MAX_VALUE)
    }
}
