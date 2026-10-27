package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

val pulseIconCache = mutableMapOf<Color, ImageVector>()

fun pulseIcon(strokeColor: Color): ImageVector = pulseIconCache.getOrPut(strokeColor) {
    strokePaths("pulseIcon", strokeColor, "M3 12h4l3-8 4 16 3-8h4")
}
