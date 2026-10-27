package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

val noteIconCache = mutableMapOf<Color, ImageVector>()

fun noteIcon(strokeColor: Color): ImageVector = noteIconCache.getOrPut(strokeColor) {
    strokePaths("noteIcon", strokeColor, "M5 4h10l4 4v12H5z", "M14 4v5h5M8 13h8M8 17h5")
}
