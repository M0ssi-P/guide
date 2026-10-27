package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

val headphonesIconCache = mutableMapOf<Color, ImageVector>()

fun headphonesIcon(strokeColor: Color): ImageVector = headphonesIconCache.getOrPut(strokeColor) {
    strokePaths("headphonesIcon", strokeColor, "M4 14v-2a8 8 0 0 1 16 0v2", "M4.5 14h1a1.5 1.5 0 0 1 1.5 1.5v3a1.5 1.5 0 0 1-1.5 1.5h-1A1.5 1.5 0 0 1 3 18.5v-3A1.5 1.5 0 0 1 4.5 14z", "M18.5 14h1a1.5 1.5 0 0 1 1.5 1.5v3a1.5 1.5 0 0 1-1.5 1.5h-1a1.5 1.5 0 0 1-1.5-1.5v-3a1.5 1.5 0 0 1 1.5-1.5z")
}
