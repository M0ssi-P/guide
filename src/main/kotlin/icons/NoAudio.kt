package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

val noAudioIconCache = mutableMapOf<Color, ImageVector>()

fun noAudioIcon(strokeColor: Color): ImageVector = noAudioIconCache.getOrPut(strokeColor) {
    strokePaths("noAudioIcon", strokeColor, "M11 5L6 9H3v6h3l5 4z", "M22 9l-6 6M16 9l6 6")
}
