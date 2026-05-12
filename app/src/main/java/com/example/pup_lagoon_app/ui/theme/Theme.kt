package com.example.pup_lagoon_app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = lightColorScheme(
    primary = Maroon,
    onPrimary = Color.White,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

private val LightColorScheme = lightColorScheme(
    primary = Maroon,
    onPrimary = Color.White,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun PuplagoonappTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Force light-themed appearance regardless of system setting to "disable" dark mode for now.
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}