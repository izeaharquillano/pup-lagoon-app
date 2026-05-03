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
