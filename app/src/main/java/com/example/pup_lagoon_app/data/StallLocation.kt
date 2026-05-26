package com.example.pup_lagoon_app.data

import androidx.compose.ui.geometry.Offset

data class StallLocation(
    val stallId: String,
    val pixelX: Float,
    val pixelY: Float
) {
    fun toOffset() = Offset(pixelX, pixelY)
}
