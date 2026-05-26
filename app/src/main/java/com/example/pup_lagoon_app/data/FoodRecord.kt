package com.example.pup_lagoon_app.data

data class FoodRecord(
    val stallId: String,
    val stallName: String,
    val foodId: String,
    val name: String,
    val price: String,
    val numericPrice: Double,
    val categories: List<String>
)

// this is for the same item, different sizes type results
data class MergedRecords(
    val id: String, // Unique identifier for stable list keys
    val baseName: String,
    val stallName: String,
    val categories: List<String>,
    val sizePrices: List<String>, // Formatted as "Size - ₱Price"
    val priceRange: String
)