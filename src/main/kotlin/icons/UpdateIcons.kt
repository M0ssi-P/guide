package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

val updateArrowIconCache = mutableMapOf<Color, ImageVector>()

fun updateArrowIcon(strokeColor: Color): ImageVector = updateArrowIconCache.getOrPut(strokeColor) {
    strokePaths("updateArrowIcon", strokeColor, "M12 4v11", "M7 10l5 5 5-5", "M5 20h14", width = 2f)
}

val restoreIconCache = mutableMapOf<Color, ImageVector>()

fun restoreIcon(strokeColor: Color): ImageVector = restoreIconCache.getOrPut(strokeColor) {
    strokePaths("restoreIcon", strokeColor, "M4 12a8 8 0 1 0 2.3-5.7", "M4 4v4h4", width = 2f)
}

val shieldIconCache = mutableMapOf<Color, ImageVector>()

fun shieldIcon(strokeColor: Color): ImageVector = shieldIconCache.getOrPut(strokeColor) {
    strokePaths("shieldIcon", strokeColor, "M12 3l8 3v6c0 4.5-3.4 8-8 9-4.6-1-8-4.5-8-9V6z", "M9 12l2 2 4-4")
}
