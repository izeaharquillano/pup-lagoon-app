package com.example.pup_lagoon_app.data

import androidx.compose.ui.geometry.Offset

enum class LabelType {
    BUILDING,
    STREET,
    LANDMARK
}

data class MapLabel(
    val id: String,
    val text: String,
    val pixelX: Float,
    val pixelY: Float,
    val type: LabelType,
    val rotation: Float = 0f
) {
    fun toOffset() = Offset(pixelX, pixelY)
}
