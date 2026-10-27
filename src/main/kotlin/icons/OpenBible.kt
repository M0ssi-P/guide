package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

val openBibleIconCache = mutableMapOf<Color, ImageVector>()

fun openBibleIcon(strokeColor: Color): ImageVector = openBibleIconCache.getOrPut(strokeColor) {
    strokePaths("openBibleIcon", strokeColor, "M4 6l8-2 8 2v13l-8-2-8 2z", "M12 4v13")
}
