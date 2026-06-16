package com.example.pup_lagoon_app.data

import android.content.Context
import androidx.compose.ui.geometry.Offset
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

class FoodRepository(private val context: Context) {
    private val nameTree = BTree<String, FoodRecord>(5)
    private val categoryTree = BTree<String, FoodRecord>(5)
    private val priceTree = BTree<Double, FoodRecord>(5)
    private val allCategories = mutableSetOf<String>()
    private val stallLocations = mutableMapOf<String, StallLocation>()
    private val mapLabels = mutableListOf<MapLabel>()

    init {
        loadStallLocations()
        loadMapLabels()
        loadFromCsv()
    }

    private fun loadMapLabels() {
        try {
            val inputStream = context.assets.open("map_labels.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.readLine() // Skip header
            
            var line: String? = reader.readLine()
            while (line != null) {
                val tokens = line.split(",")
                if (tokens.size >= 5) {
                    val id = tokens[0].trim()
                    val text = tokens[1].trim()
                    val x = tokens[2].trim().toFloatOrNull() ?: 0f
                    val y = tokens[3].trim().toFloatOrNull() ?: 0f
                    val typeStr = tokens[4].trim().uppercase()
                    val type = try { LabelType.valueOf(typeStr) } catch (e: Exception) { LabelType.LANDMARK }
                    val rotation = if (tokens.size >= 6) tokens[5].trim().toFloatOrNull() ?: 0f else 0f
                    
                    mapLabels.add(MapLabel(id, text, x, y, type, rotation))
                }
                line = reader.readLine()
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
                record.name.contains(queryLower, ignoreCase = true) ||
                record.stallName.contains(queryLower, ignoreCase = true) ||
                record.stallId.equals(queryLower, ignoreCase = true) ||
                record.stallId.trimStart('0').equals(queryLower.trimStart('0'), ignoreCase = true)
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

    fun getAllStallLocations(): List<StallLocation> {
        return stallLocations.values.toList()
    }

    fun getMapLabels(): List<MapLabel> {
        return mapLabels
    }

    fun findNearestGate(target: Offset): MapLabel? {
        return mapLabels
            .filter { it.type == LabelType.LANDMARK && it.text.contains("Gate", ignoreCase = true) }
            .minByOrNull { label ->
                val dx = label.pixelX - target.x
                val dy = label.pixelY - target.y
                dx * dx + dy * dy
            }
    }

    fun calculatePath(from: MapLabel, to: StallLocation): List<Offset> {
        val path = mutableListOf<Offset>()
        path.add(Offset(from.pixelX, from.pixelY))

        // Full logical sequence of stalls around the lagoon in physical order
        // Note: 15 and 16 are swapped because physically 15 comes after 17 and before 16.
        val fullLoop = listOf(
            "27", "26", "25", "24", "23", "22", "21", "20", "19", "18", "17", "15", "16", "14", "13", 
            "12", "11", "10", "09", "08", "07", "06", "05", "04", "03", "02", "01"
        )
        val destId = to.stallId
        
        fun getStallOffset(id: String) = stallLocations[id]?.toOffset()

        when {
            from.text.contains("Gate 3") -> {
                // Gate 3 (West) connects to the walkway hub near the top of the West side
                path.add(Offset(1419f, 1214f)) 
                
                // Join the sequence at 27 and follow it step-by-step
                val idx = fullLoop.indexOf(destId)
                if (idx != -1) {
                    for (i in 0..idx) {
                        getStallOffset(fullLoop[i])?.let { path.add(it) }
                    }
                }
            }
            from.text.contains("Gate 2") -> {
                // Gate 2 (South) connects to the South walkway hub
                path.add(Offset(1636f, 1768f))
                
                // This hub is near Stall 21.
                // Determine direction: towards 27 (higher IDs) or 01 (lower IDs)
                if (destId.toInt() >= 21) {
                    // Go towards 27 (backwards in our loop)
                    val seq = (21..27).map { String.format(Locale.US, "%02d", it) }
                    val idx = seq.indexOf(destId)
                    for (i in 0..idx) { getStallOffset(seq[i])?.let { path.add(it) } }
                } else {
                    // Go towards 01 (forwards in our loop)
                    val startIdx = fullLoop.indexOf("21")
                    val endIdx = fullLoop.indexOf(destId)
                    for (i in startIdx..endIdx) { getStallOffset(fullLoop[i])?.let { path.add(it) } }
                }
            }
            from.text.contains("Gate 1") -> {
                // Gate 1 (East) connects to the East walkway hub
                path.add(Offset(2139f, 1360f))
                
                // This hub is between 12 and 13.
                if (destId.toInt() <= 12) {
                    // Go UP towards 01
                    val seq = (12 downTo 1).map { String.format(Locale.US, "%02d", it) }
                    val idx = seq.indexOf(destId)
                    for (i in 0..idx) { getStallOffset(seq[i])?.let { path.add(it) } }
                } else {
                    // Go DOWN towards 13, 14... 27
                    val startIdx = fullLoop.indexOf("12")
                    val endIdx = fullLoop.indexOf(destId)
                    for (i in startIdx..endIdx) { getStallOffset(fullLoop[i])?.let { path.add(it) } }
                }
            }
        }

        path.add(to.toOffset())
        return path.distinct()
    }

    fun getDirectionText(start: MapLabel, end: StallLocation): String {
        val gateName = start.text
        return when {
            gateName.contains("Gate 1") -> {
                // Gate 1 is on the East. Stalls are West.
                val isNorth = end.pixelY < 1360
                val turn = if (isNorth) "turn right" else "turn left"
                "From $gateName, enter and $turn along the path to reach the stall."
            }
            gateName.contains("Gate 2") -> {
                // Gate 2 is South-ish. Stalls are North-ish.
                val isNorth = end.pixelX > 1636
                val turn = if (isNorth) "turn hard right" else "turn left"
                "From $gateName, head forward then $turn towards the lagoon."
            }
            gateName.contains("Gate 3") -> {
                // Gate 3 is West-ish. Stalls are East-ish.
                val isNorth = end.pixelY < 1214
                val turn = if (isNorth) "turn left" else "turn right"
                "From $gateName, enter and $turn to find the stall near the building."
            }
            else -> "From $gateName, follow the path to the stall location."
        }
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

    fun getSharedPreferences(): android.content.SharedPreferences {
        return context.getSharedPreferences("pup_lagoon_prefs", Context.MODE_PRIVATE)
    }
}
