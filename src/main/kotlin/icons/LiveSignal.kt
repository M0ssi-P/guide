package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

val liveSignalIconCache = mutableMapOf<Color, ImageVector>()

fun liveSignalIcon(strokeColor: Color): ImageVector = liveSignalIconCache.getOrPut(strokeColor) {
    strokePaths("liveSignalIcon", strokeColor, "M5 12a7 7 0 0 1 14 0M8.5 12a3.5 3.5 0 0 1 7 0", "M12 11a1 1 0 1 1 0 2a1 1 0 1 1 0-2z", "M12 13v7")
}
